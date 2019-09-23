import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class SummaryMain {

    // 基础目录
    private static String basePath = new File(SummaryMain.class.getClassLoader().getResource("").getPath()).getParent();

    // xls文件所在目录
    private static String xlsPath = basePath + "/xls";

    // 生成的txt文件所在目录
    private static String txtPath = basePath + "/txt";

    // 配置文件路径
    private static String confFilePath = basePath + "/config/conf.properties";

    // 汇总统计目录
    private static String summaryPath = basePath + "/summary";

    // 配置文件
    private static Properties config = Xls2TxtMain.readConfig(confFilePath);

    // 一共4组
    private static final int groupNum = Integer.parseInt(config.getProperty("groupNum", "4"));

    // 每组10行
    private static final int rowNum = Integer.parseInt(config.getProperty("rowNum", "10"));

    // 每行3列，X,Y,Z
    private static final int stepNum = Integer.parseInt(config.getProperty("stepNum", "3"));

    // 换算单位(1000)
    private static final double unit = Double.parseDouble(config.getProperty("unit", "1000"));


    public static void main(String[] args) throws Exception {
        // 获取txt文件
        ArrayList<File> txtFiles = getTxtFiles(txtPath);
        //System.out.println(Xls2TxtMain.class.getClassLoader().getResource("").getPath());
        System.out.println(txtFiles);
        // 读取txt
        Map[] datas = readTxt(txtFiles);
        //System.out.println(datas);
        // 生成汇总xls(Max)
        txt2Summary("summary_max.txt", datas[0]);
        // 生成汇总xls(Min)
        txt2Summary("summary_min.txt", datas[1]);
    }

    public static void txt2Summary(String fileName, Map<String, String> datas) {
        PrintWriter pw = null;
        try {
            File path = new File(summaryPath);
            if (!path.exists()) {
                path.mkdirs();
            }
            String summaryFilename = summaryPath + "/" + fileName;
            pw = new PrintWriter(new File(summaryFilename), "UTF-8");
            int cnum = 0;
            for (Map.Entry<String, String> entry : datas.entrySet()) {
                String key = entry.getKey();
                String rowData = entry.getValue();
                System.out.println("rowData "+ key + ":"+ rowData);
                pw.write(rowData);
                if ((cnum % 6) == 5) {
                    pw.write("\n");
                } else {
                    pw.write(" ");
                }
                cnum = cnum + 1;
            }
            pw.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    public static Map[] readTxt(ArrayList<File> txtFiles) throws Exception {
        // 最大值集合
        Map<String, String> maxDatas = new LinkedHashMap<>();
        // 最小值集合
        Map<String, String> minDatas = new LinkedHashMap<>();
        Map[] datas = new Map[]{maxDatas, minDatas};

        for (File txtFile : txtFiles) {
            String fileName = txtFile.getName();
            String fname = fileName.substring(0, fileName.lastIndexOf("."));
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(txtFile), "UTF-8"));
                String record = null;
                int rownum = 1;
                while ((record = br.readLine()) != null) {
                    record = record.trim();
                    if (!"".equals(record)) {
                        String[] recordarry = record.split(" ");
                        if (recordarry.length > 1) {
                            for (int i = 0; i < recordarry.length; i++) {
                                String key = rownum + "_" + i;
                                String value = recordarry[i];
                                if (value != null && !"".equals(value)) {
                                    double dv = Double.parseDouble(value);
                                    if (maxDatas.containsKey(key)) {
                                        String mv = maxDatas.get(key);
                                        String nv = minDatas.get(key);
                                        if (i <= 2) {
                                            String[] mt = mv.split(":");
                                            double mt0 = Double.parseDouble(mt[0]);
                                            if (Math.abs(mt0) == Math.abs(dv)) {
                                                maxDatas.put(key, maxDatas.get(key) + "," + fname);
                                            } else if (Math.abs(mt0) < Math.abs(dv)) {
                                                maxDatas.put(key, value + ":" + fname);
                                            }

                                            String[] nt = nv.split(":");
                                            double nt0 = Double.parseDouble(nt[0]);
                                            if (Math.abs(nt0) == Math.abs(dv)) {
                                                minDatas.put(key, minDatas.get(key) + "," + fname);
                                            } else if (Math.abs(nt0) > Math.abs(dv)) {
                                                minDatas.put(key, value + ":" + fname);
                                            }
                                        }
                                    } else {
                                        if (i > 2) {
                                            maxDatas.put(key, "0");
                                            minDatas.put(key, "0");
                                        } else {
                                            maxDatas.put(key, value + ":" + fname);
                                            minDatas.put(key, value + ":" + fname);
                                        }
                                    }
                                }
                            }
                            rownum = rownum + 1;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        return datas;
    }

    public static ArrayList<File> getTxtFiles(String path) throws Exception {
        ArrayList<File> fileList = new ArrayList<>();
        File file = new File(path);
        System.out.println(file.getParent());
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File fileIndex : files) {
                if (!fileIndex.isDirectory()) {
                    String fileName = fileIndex.getName();
                    String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
                    if ("txt".equalsIgnoreCase(suffix)) {
                        fileList.add(fileIndex);
                    }
                }
            }
        }
        return fileList;
    }
}
