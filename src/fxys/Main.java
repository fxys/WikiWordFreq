package fxys;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class Main {
    public static double percent = 0.1;

    public static void main(String[] args) {
        long t = System.nanoTime();
        percent /= 100;
        System.out.println();
        Map<String, Long> m = getMap();
        List<String> l = getArticleList();
        if (l == null || m == null) return;
        int len = l.size();
        int cut = (int) (len * percent);
        Random r = new Random();

        for (int i = 0; i < len; i += (1 / percent) + ((percent != 100) ? r.nextInt(5) : 0)) {
            long start = System.nanoTime();
            String path = getArticle(l.get(i));
            if (path == null) return;
            try {
                BufferedReader bf = new BufferedReader(new FileReader(path));
                List<String> list = new ArrayList<>();
                String line = bf.readLine();

                while (line != null) {
                    list.add(line);
                    line = bf.readLine();
                }
                bf.close();
                for (String s : list) {
                    String[] words = s.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", "").replace("\\n", " ").replaceAll("\\[(.*?)]", "").split(" References")[0].replaceAll("[^a-zA-Z\\s'-]+", "").toLowerCase(Locale.ROOT).split("\\s+");
                    for (String w : words) {
                        if (m.containsKey(w)) {
                            long n = m.get(w);
                            m.put(w, n + 1);
                        }
                    }
                }
            } catch (IOException ignored) {
            }
            long dur = System.nanoTime() - start;
            progressBar(((double)i / len) * 100, cut, "Currently reading: " + l.get(i), dur * (cut - ((double)i/len) * 100));
        }
        progressBar(100, cut, "DONE", 0);
        System.out.println("\n");
        m = sortByValue(m);
        long sum = 0;
        for (long n : m.values())
            sum += n;
        long t1 = System.nanoTime();
        System.out.println("Time: " + (t1 - t) / 1000000.0 + "ms");

        System.out.println("\nThe most common words in this set was are:");
        HashMap<String, Long> top = getMostCommonWords(m);
        int pos = 1;
        for (String key : top.keySet()) {
            long n = top.get(key);
            System.out.printf("%d: '%s' with %d which occupies %f%%%n", pos, key, n, ((float)n / sum) * 100);
            pos++;
        }
    }

    public static void progressBar(double percent, int max, String curr, double time) {
        DecimalFormat df = new DecimalFormat("###.##");
        StringBuilder bar = new StringBuilder("ET remaining: " + df.format(time / 1000000000) + "s [");

        for (int i = 0; i < 50; i++) {
            if (i < (Math.floor(percent / 2)))
                bar.append("=");
            else if (i == (Math.floor(percent / 2)))
                bar.append(">");
            else
                bar.append(" ");
        }

        bar.append("]  ").append(df.format(percent)).append("%  (").append((int)(max * (percent / 100))).append("/").append(max).append(")  ").append(curr);
        System.out.print("\r" + bar);
    }

    public static List<String> getArticleList() {
        try {
            URL url = new URL("https://dumps.wikimedia.org/enwiki/latest/enwiki-latest-all-titles-in-ns0.gz");

            String temp = Paths.get(System.getProperty("java.io.tmpdir"), new File(url.getPath()).getName()).toString();
            String out = Paths.get(System.getProperty("java.io.tmpdir"), "wiki-title-list").toString();

            if (!new File(temp).exists())
                download(url, temp);
            if (!new File(out).exists())
                decompressGZ(temp, out);

            List<String> list = new ArrayList<>();
            BufferedReader bf = new BufferedReader(new FileReader(out));
            bf.readLine();
            String line = bf.readLine();

            while (line != null) {
                list.add(line);
                line = bf.readLine();
            }
            bf.close();
            return list;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, Long> getMap() {
        try {
            List<String> list = new ArrayList<>();
            BufferedReader bf = new BufferedReader(new FileReader("words.txt"));
            bf.readLine();
            String line = bf.readLine();

            while (line != null) {
                list.add(line);
                line = bf.readLine();
            }
            bf.close();
            Map<String, Long> map = new HashMap<>();
            for (String s : list) {
                s = s.toLowerCase(Locale.ROOT).strip();
                if (!map.containsKey(s))
                    map.put(s, 0L);
            }
            return map;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static HashMap<String, Long> sortByValue(Map<String, Long> map) {
        List<Map.Entry<String, Long>> list = new LinkedList<>(map.entrySet());

        list.sort(Map.Entry.comparingByValue());

        HashMap<String, Long> map1 = new LinkedHashMap<>();
        for (Map.Entry<String, Long> e : list)
            map1.put(e.getKey(), e.getValue());
        return map1;
    }

    public static HashMap<String, Long> getMostCommonWords(Map<String, Long> map) {
        List<Map.Entry<String, Long>> list = new ArrayList<>(map.entrySet());

        HashMap<String, Long> map1 = new LinkedHashMap<>();
        for(int i = 1; i < 21; i++) {
            Map.Entry<String, Long> e = list.get(list.size() - i);
            map1.put(e.getKey(), e.getValue());
        }
        return map1;
    }

    public static String getArticle(String title) {
        try {
            URL url = new URL("https://en.wikipedia.org/w/api.php?action=parse&page=" + title + "&prop=text&formatversion=2&format=json");
            String temp = Paths.get(System.getProperty("java.io.tmpdir"), new File(url.getPath()).getName()).toString();
            download(url, temp);
            return temp;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void download(URL source, String target) {
        try {
            BufferedInputStream is = new BufferedInputStream(source.openStream());
            FileOutputStream fos = new FileOutputStream(target);

            byte[] dataBuffer = new byte[1024];
            int bytes;
            while ((bytes = is.read(dataBuffer, 0, 1024)) != -1)
                fos.write(dataBuffer, 0, bytes);

            is.close();
            fos.close();
        } catch (IOException ignored) {
        }
    }

    public static void decompressGZ(String source, String target) {
        try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(source))) {
            Files.copy(gis, Path.of(target));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}