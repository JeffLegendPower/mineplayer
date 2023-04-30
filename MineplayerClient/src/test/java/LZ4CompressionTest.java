import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class LZ4CompressionTest {

    @Test
    public void testLZ4Compression() throws IOException {
        InputStream input = new FileInputStream("C:\\Users\\goyal_hfho3dz\\IdeaProjects\\MineplayerClient\\src\\test\\resources\\superlongbase64image.txt");
        byte[] testBytes = input.readAllBytes();

        long startTime = System.currentTimeMillis();
        LZ4Factory factory = LZ4Factory.fastestInstance();
        LZ4Compressor compressor = factory.fastCompressor();
        long endTime = System.currentTimeMillis();
        System.out.println("Time to initialize compressor: " + (endTime - startTime) + "ms");

        startTime = System.currentTimeMillis();
//        int maxCompressedLength = compressor.maxCompressedLength(testBytes.length);

//        byte[] compressed = new byte[maxCompressedLength];
//        compressor.compress(testBytes, 0, testBytes.length, compressed, 0, maxCompressedLength);
        byte[] compressed = compressor.compress(testBytes);
        endTime = System.currentTimeMillis();
        System.out.println("Time to compress bytes: " + (endTime - startTime) + "ms");

        System.out.println("Compressed size: " + compressed.length + " bytes, " + (compressed.length * 100 / (double) testBytes.length) + "% of original size");

        int numDefaultBytes = 0;
        for (byte b : compressed) {
            if (b == 0) numDefaultBytes++;
        }

        System.out.println("Number of default bytes: " + numDefaultBytes + " bytes, " + (numDefaultBytes * 100 / (double) compressed.length) + "% of compressed size");


    }
}
