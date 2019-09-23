import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Xls2TxtMain {

    // 基础目录
    private static String basePath = new File(Xls2TxtMain.class.getClassLoader().getResource("").getPath()).getParent();

    // xls文件所在目录
    private static String xlsPath = basePath + "/xls";

    // 生成的txt文件所在目录
    private static String txtPath = basePath + "/txt";

    // 配置文件路径
    private static String confFilePath = basePath + "/config/conf.properties";

    // 配置文件
    private static Properties config = readConfig(confFilePath);

    // 一共4组
    private static final int groupNum = Integer.parseInt(config.getProperty("groupNum", "4"));

    // 每组10行
    private static final int rowNum = Integer.parseInt(config.getProperty("rowNum", "10"));

    // 每行3列，X,Y,Z
    private static final int stepNum = Integer.parseInt(config.getProperty("stepNum", "3"));

    // 换算单位(1000)
    private static final double unit = Double.parseDouble(config.getProperty("unit", "1000"));


    public static void main(String[] args) throws Exception {
        ArrayList<File> xlsFiles = getXlsFiles(xlsPath);
        //System.out.println(Xls2TxtMain.class.getClassLoader().getResource("").getPath());
        System.out.println(xlsFiles);
        for (File xlsFile : xlsFiles) {
            String fileName = xlsFile.getName();
            String fname = fileName.substring(0, fileName.lastIndexOf("."));
            // 读取xls
            Map<String, Map<Integer, List<Double>>> datas = readXls(xlsFile);
            System.out.println(datas);
            // xls转txt
            xls2txt(fname, datas);
        }
    }

    public static Properties readConfig(String confFilePath) {
        Properties properties = new Properties();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(confFilePath));
            properties.load(bufferedReader);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return properties;
    }

    public static void xls2txt(String fileName, Map<String, Map<Integer, List<Double>>> datas) {
        for (Map.Entry<String, Map<Integer, List<Double>>> entry : datas.entrySet()) {
            PrintWriter pw = null;
            try {
                String sheetName = entry.getKey();
                Map<Integer, List<Double>> sheetDatas = entry.getValue();
                System.out.println(sheetName + ":" + sheetDatas);
                String txtFilename = txtPath + "/" + fileName + "_" + sheetName + ".txt";
                pw = new PrintWriter(new File(txtFilename));
                for (Map.Entry<Integer, List<Double>> data : sheetDatas.entrySet()) {
                    Integer groupNum = data.getKey();
                    // 偏移量X,Y,Z
                    String[] offsetArray = config.getProperty(groupNum.toString(), "0,0,0").split(",");

                    List<Double> tmpData = data.getValue();
                    for (int i = 0; i < tmpData.size(); i++) {
                        Double v = tmpData.get(i);
                        int flag = i % stepNum;
                        Double offset = Double.parseDouble(offsetArray[flag]);
                        Double retd = (v + offset) / unit;
                        BigDecimal ret = new BigDecimal(retd);
                        ret = ret.setScale(3, BigDecimal.ROUND_HALF_UP);
                        System.out.println("v:" + v + ",offset:" + offset + ",unit:" + unit + ",ret:" + ret);
                        pw.write(ret.toPlainString());
                        if (flag == 2) {
                            pw.write(" 0 0 0\n");
                        } else {
                            pw.write(" ");
                        }
                    }
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
    }

    public static Map<String, Map<Integer, List<Double>>> readXls(File xlsFile) throws Exception {
        Map<String, Map<Integer, List<Double>>> datas = new LinkedHashMap<>();
        Workbook wb = null;
        try {
            wb = WorkbookFactory.create(xlsFile);
            int numberOfSheets = wb.getNumberOfSheets();
            if (numberOfSheets > 0) {
                for (int i = 0; i < numberOfSheets; i++) {
                    Sheet sheet = wb.getSheetAt(i);
                    if (sheet != null) {
                        Map<Integer, List<Double>> sheetDatas = new LinkedHashMap<>();
                        datas.put(sheet.getSheetName(), sheetDatas);
                        // 获得当前sheet的结束行
                        int lastRowNum = sheet.getLastRowNum() + 1;
                        System.out.println("lastRowNum:" + lastRowNum);
                        int flagNum = 0;
                        if (lastRowNum > 0) {
                            for (int j = 0; j < lastRowNum; j++) {
                                Row row = sheet.getRow(j);
                                if (row != null) {
                                    short lastCellNum = row.getLastCellNum();
                                    System.out.println("lastCellNum:" + lastCellNum);
                                    if (lastCellNum > 0) {
                                        for (int k = 0; k < lastCellNum; k++) {
                                            Cell cell = row.getCell(k);
                                            if (cell != null) {
                                                CellType cellType = cell.getCellType();
                                                if (cellType.equals(CellType.STRING)) {
                                                    String value = cell.getStringCellValue();
                                                    if ("POSX".equals(value)) {
                                                        flagNum = flagNum + 1;
                                                        System.out.println(flagNum + ":" + cell.getStringCellValue());
                                                    }
                                                    break;
                                                } else if (cellType.equals(CellType.NUMERIC)) {
                                                    if (flagNum > 0) {
                                                        double numericCellValue = cell.getNumericCellValue();
                                                        System.out.println(flagNum + ":" + numericCellValue);
                                                        List<Double> data;
                                                        if (sheetDatas.containsKey(flagNum)) {
                                                            data = sheetDatas.get(flagNum);
                                                        } else {
                                                            data = new ArrayList<>();
                                                            sheetDatas.put(flagNum, data);
                                                        }
                                                        data.add(numericCellValue);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            if (wb != null) {
                try {
                    wb.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return datas;
    }

    public static ArrayList<File> getXlsFiles(String path) throws Exception {
        ArrayList<File> fileList = new ArrayList<>();
        File file = new File(path);
        System.out.println(file.getParent());
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File fileIndex : files) {
                if (!fileIndex.isDirectory()) {
                    String fileName = fileIndex.getName();
                    String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
                    if ("xls".equalsIgnoreCase(suffix) || "xlsx".equalsIgnoreCase(suffix)) {
                        fileList.add(fileIndex);
                    }
                }
            }
        }
        return fileList;
    }
}
