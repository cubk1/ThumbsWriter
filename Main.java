import javax.imageio.*;
import javax.imageio.stream.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.*;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Params: <Input> <Input2> <Output>");
            System.exit(1);
        }
        File mainImageFile = new File(args[0]);
        File thumbnailImageFile = new File(args[1]);
        File outputFile = new File(args[2] + ".jpg");

        if (!mainImageFile.exists() || !thumbnailImageFile.exists()) {
            System.err.println("Failed to find input files.");
            System.exit(1);
        }
        byte[] resultBytes = writeThumbs(mainImageFile, thumbnailImageFile);
        Files.write(outputFile.toPath(), resultBytes);
        System.out.println("Output written: " + outputFile.getAbsolutePath() + " (" + resultBytes.length + " bytes)");
    }

    static byte[] writeThumbs(File mainImageFile, File thumbnailSourceFile) throws Exception {
        byte[] offset = {
                77, 77, 0, 42, 0, 0, 0, 8, 0, 7, 1, 18, 0, 3, 0, 0,
                0, 1, 0, 1, 0, 0, 1, 26, 0, 5, 0, 0, 0, 1, 0, 0,
                0, 98, 1, 27, 0, 5, 0, 0, 0, 1, 0, 0, 0, 106, 1, 40,
                0, 3, 0, 0, 0, 1, 0, 2, 0, 0, 1, 49, 0, 2, 0, 0,
                0, 12, 0, 0, 0, 114, 1, 50, 0, 2, 0, 0, 0, 20, 0, 0,
                0, 126, -121, 105, 0, 4, 0, 0, 0, 1, 0, 0, 0, -110, 0, 0,
                0, -44, 0, 0, 1, 44, 0, 0, 0, 1, 0, 0, 1, 44, 0, 0,
                0, 1, 71, 73, 77, 80, 32, 50, 46, 56, 46, 49, 52, 0, 50, 48,
                49, 53, 58, 48, 52, 58, 48, 52, 32, 49, 51, 58, 48, 55, 58, 49,
                49, 0, 0, 5, -112, 0, 0, 7, 0, 0, 0, 4, 48, 50, 50, 49,
                -96, 0, 0, 7, 0, 0, 0, 4, 48, 49, 48, 48, -96, 1, 0, 3,
                0, 0, 0, 1, -1, -1, 0, 0, -96, 2, 0, 4, 0, 0, 0, 1,
                0, 0, 3, 98, -96, 3, 0, 4, 0, 0, 0, 1, 0, 0, 4, -55,
                0, 0, 0, 0, 0, 6, 1, 3, 0, 3, 0, 0, 0, 1, 0, 6,
                0, 0, 1, 26, 0, 5, 0, 0, 0, 1, 0, 0, 1, 34, 1, 27,
                0, 5, 0, 0, 0, 1, 0, 0, 1, 42, 1, 40, 0, 3, 0, 0,
                0, 1, 0, 2, 0, 0, 2, 1, 0, 4, 0, 0, 0, 1, 0, 0,
                1, 50, 2, 2, 0, 4, 0, 0, 0, 1, 0, 0, 27, -49, 0, 0,
                0, 0, 0, 0, 0, 72, 0, 0, 0, 1, 0, 0, 0, 72, 0, 0,
                0, 1
        };

        BufferedImage mainImage = ImageIO.read(mainImageFile);
        int mainWidth = mainImage.getWidth();
        int mainHeight = mainImage.getHeight();
        BufferedImage mainCanvas = new BufferedImage(mainWidth, mainHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D mainGraphics = mainCanvas.createGraphics();
        mainGraphics.setColor(Color.WHITE);
        mainGraphics.fillRect(0, 0, mainWidth, mainHeight);
        mainGraphics.drawImage(mainImage, 0, 0, null);
        mainGraphics.dispose();

        BufferedImage thumbnailSource = ImageIO.read(thumbnailSourceFile);
        int thumbnailHeight = (int) (138.0 * mainHeight / mainWidth);
        BufferedImage thumbnailCanvas = new BufferedImage(138, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D thumbnailGraphics = thumbnailCanvas.createGraphics();
        thumbnailGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        thumbnailGraphics.setColor(Color.WHITE);
        thumbnailGraphics.fillRect(0, 0, 138, thumbnailHeight);
        thumbnailGraphics.drawImage(thumbnailSource, 0, 0, 138, thumbnailHeight, null);
        thumbnailGraphics.dispose();

        ByteArrayOutputStream thumbnailOutputStream = new ByteArrayOutputStream();
        ImageWriter thumbnailWriter = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam thumbnailWriteParam = thumbnailWriter.getDefaultWriteParam();
        thumbnailWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        thumbnailWriteParam.setCompressionQuality(0.85f);
        thumbnailWriter.setOutput(new MemoryCacheImageOutputStream(thumbnailOutputStream));
        thumbnailWriter.write(null, new IIOImage(thumbnailCanvas, null, null), thumbnailWriteParam);
        thumbnailWriter.dispose();
        byte[] output = thumbnailOutputStream.toByteArray();

        byte[] toWrite = offset.clone();
        int thumbnailLength = output.length;
        toWrite[282] = (byte) ((thumbnailLength >> 24) & 0xFF);
        toWrite[283] = (byte) ((thumbnailLength >> 16) & 0xFF);
        toWrite[284] = (byte) ((thumbnailLength >> 8) & 0xFF);
        toWrite[285] = (byte) (thumbnailLength & 0xFF);

        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        arrayOutputStream.write(new byte[]{69, 120, 105, 102, 0, 0});
        arrayOutputStream.write(toWrite);
        arrayOutputStream.write(output);
        byte[] body = arrayOutputStream.toByteArray();

        int app1SegmentLength = body.length + 2;
        ByteArrayOutputStream app1SegmentStream = new ByteArrayOutputStream();
        app1SegmentStream.write(0xFF);
        app1SegmentStream.write(0xE1);
        app1SegmentStream.write((app1SegmentLength >> 8) & 0xFF);
        app1SegmentStream.write(app1SegmentLength & 0xFF);
        app1SegmentStream.write(body);

        ByteArrayOutputStream mainJpegOutputStream = new ByteArrayOutputStream();
        ImageWriter mainWriter = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam mainWriteParam = mainWriter.getDefaultWriteParam();
        mainWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        mainWriteParam.setCompressionQuality(0.9f);
        mainWriter.setOutput(new MemoryCacheImageOutputStream(mainJpegOutputStream));
        mainWriter.write(null, new IIOImage(mainCanvas, null, null), mainWriteParam);
        mainWriter.dispose();
        byte[] mainJpegBytes = mainJpegOutputStream.toByteArray();

        int i = 2;
        while (i + 3 < mainJpegBytes.length) {
            int markerSecondByte = mainJpegBytes[i + 1] & 0xFF;
            boolean isAppOrCommentMarker = mainJpegBytes[i] == (byte) 0xFF && ((markerSecondByte >= 0xE0 && markerSecondByte <= 0xEF) || markerSecondByte == 0xFE);
            if (isAppOrCommentMarker) {
                i += 2 + ((mainJpegBytes[i + 2] & 0xFF) << 8 | (mainJpegBytes[i + 3] & 0xFF));
            } else {
                break;
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(new byte[]{(byte) 0xFF, (byte) 0xD8});
        outputStream.write(app1SegmentStream.toByteArray());
        outputStream.write(mainJpegBytes, i, mainJpegBytes.length - i);
        return outputStream.toByteArray();
    }
}
