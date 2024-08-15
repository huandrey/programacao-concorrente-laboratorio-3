import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;

public class FileSimilarity {

    private static final int MAX_THREADS = 100;
    private static final Semaphore semaphore = new Semaphore(MAX_THREADS);


    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java Sum filepath1 filepath2 filepathN");
            System.exit(1);
        }

        // Create a map to store the fingerprint for each file
        Map<String, List<Long>> fileFingerprints = new HashMap<>();

        List<Thread> threads = new ArrayList<>();


        // Calculate the fingerprint for each file concurrently
        for (String path : args) {

            Thread thread = new Thread(() -> {
                try {
                    semaphore.acquire();

                    List<Long> fingerprint = fileSum(path);

                    synchronized (fileFingerprints) {
                        fileFingerprints.put(path, fingerprint);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    semaphore.release();
                }
            });

            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        threads.clear();

      
        // Compare each pair of files
        for (int i = 0; i < args.length; i++) {
            for (int j = i + 1; j < args.length; j++) {
                String file1 = args[i];
                String file2 = args[j];
                List<Long> fingerprint1 = fileFingerprints.get(file1);
                List<Long> fingerprint2 = fileFingerprints.get(file2);
                float similarityScore = similarity(fingerprint1, fingerprint2);
                System.out.println("Similarity between " + file1 + " and " + file2 + ": " + (similarityScore * 100) + "%");
            }
        }
    }

    private static List<Long> fileSum(String filePath) throws IOException {
        File file = new File(filePath);
        List<Long> chunks = new ArrayList<>();
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[100];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                long sum = sum(buffer, bytesRead);
                chunks.add(sum);
            }
        }
        return chunks;
    }

    private static long sum(byte[] buffer, int length) {
        long sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Byte.toUnsignedInt(buffer[i]);
        }
        return sum;
    }

    private static float similarity(List<Long> base, List<Long> target) {
        int counter = 0;
        List<Long> targetCopy = new ArrayList<>(target);

        for (Long value : base) {
            if (targetCopy.contains(value)) {
                counter++;
                targetCopy.remove(value);
            }
        }

        return (float) counter / base.size();
    }
}
