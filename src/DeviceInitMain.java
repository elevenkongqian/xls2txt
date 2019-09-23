import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

public class DeviceInitMain {

    // 基础目录
    private static String basePath = new File(DeviceInitMain.class.getClassLoader().getResource("").getPath()).getParent();

    // 设备初始化xls文件所在目录
    private static String deviceInitPath = basePath + "/device_init";

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
        ArrayList<File> xlsFiles = getXlsFiles(deviceInitPath);
        //System.out.println(Xls2TxtMain.class.getClassLoader().getResource("").getPath());
        System.out.println(xlsFiles);
        for (File xlsFile : xlsFiles) {
            String fileName = xlsFile.getName();
            // 文件名就是设备id
            String deviceId = fileName.substring(0, fileName.lastIndexOf("."));
            // 读取xls
            JSONObject datas = readXls(xlsFile);
            //System.out.println(datas);
            // 读取所有模板数据
            JSONArray models = select();
            //System.out.println(models);

            for (int i = 0; i < models.size(); i++) {
                JSONObject jsonObject = models.getJSONObject(i);
                JSONObject acupoint_points = jsonObject.getJSONObject("acupoint_points");
                String model_name = jsonObject.getString("model_name");
                JSONArray rf = acupoint_points.getJSONArray("rf");
                JSONArray rfoot = foot(model_name, "rf", rf, datas);

                JSONArray lf = acupoint_points.getJSONArray("lf");
                JSONArray lfoot = foot(model_name, "lf", lf, datas);

                JSONArray rh = acupoint_points.getJSONArray("rh");
                JSONArray rhead = head(model_name, "rh", rh, datas);

                JSONArray lh = acupoint_points.getJSONArray("lh");
                JSONArray lhead = head(model_name, "lh", lh, datas);

                JSONObject ret = new JSONObject();
                ret.put("rf", rfoot);
                ret.put("lf", lfoot);
                ret.put("rh", rhead);
                ret.put("lh", lhead);

                // 入库
                //insertDB(deviceId, model_name, ret.toString());
            }
        }
    }

    private static JSONObject queryPoint(JSONArray head, int num, int offset, String model_name, String type, JSONObject datas) {
        JSONObject f;
        JSONObject jsonObject = head.getJSONObject(num);
        int p = (int) (jsonObject.getDouble("z") * 1000) - offset;
        f = datas.getJSONObject(type).getJSONObject(Integer.toString(p));
        if (f != null) {
            System.out.println("model_name:" + model_name + ", type:" + type + ", num:" + num + " , once p:" + p);
            return f;
        }
        //int fz = roundHalfUp(p);
        int fz = round(p);
        System.out.println("model_name:" + model_name + ", type:" + type + ", num:" + num + " , p:" + p + " , fz:" + fz);
        f = datas.getJSONObject(type).getJSONObject(Integer.toString(fz));
        if (f == null) {
            System.err.println("model_name:" + model_name + ", type:" + type + ", num:" + num + " , p:" + p + " , fz:" + fz);
        }
        return f;
    }

    public static JSONArray head(String model_name, String type, JSONArray head, JSONObject datas) {
        JSONObject rh0 = datas.getJSONObject(type).getJSONObject(Integer.toString(0));
        int offset = 237;
        JSONArray ret = new JSONArray();
        ret.add(rh0);
        ret.add(queryPoint(head, 2, offset, model_name, type, datas));
        ret.add(queryPoint(head, 3, offset, model_name, type, datas));
        ret.add(queryPoint(head, 7, offset, model_name, type, datas));
        //System.out.println("type:"+type+", model_name:" + model_name + " , ret:" + ret);
        return ret;
    }

    private static JSONArray foot(String model_name, String type, JSONArray foot, JSONObject datas) {
        int offset = 150;
        JSONArray ret = new JSONArray();
        ret.add(queryPoint(foot, 1, offset, model_name, type, datas));
        ret.add(queryPoint(foot, 3, offset, model_name, type, datas));
        ret.add(queryPoint(foot, 6, offset, model_name, type, datas));
        //System.out.println("type:"+type+", model_name:" + model_name + " , ret:" + ret);
        return ret;
    }

    private static int round(int t) {
        int mod = t % 10;
        if (mod >= 1 && mod <= 2) {
            return t / 10 * 10;
        }
        if (mod >= 3 && mod <= 7) {
            return t / 10 * 10 + 5;
        }
        return t / 10 * 10 + 10;
    }

    private static int roundHalfUp(int t) {
        return t % 10 >= 5 ? t / 10 * 10 + 10 : t / 10 * 10;
    }

    private static JSONArray select() {
        PreparedStatement pst = null;
        ResultSet resultSet = null;
        JSONArray datas = new JSONArray();
        Connection conn = getConnection();
        try {
            pst = conn.prepareStatement("select * from acupoint_model");
            resultSet = pst.executeQuery();
            while (resultSet.next()) {
                JSONObject data = new JSONObject();
                String name = resultSet.getString("model_name");
                String acupoint_points = resultSet.getString("acupoint_points");
                data.put("model_name", name);
                data.put("acupoint_points", JSONObject.parseObject(acupoint_points));
                datas.add(data);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (pst != null) {
                try {
                    pst.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return datas;
    }


    public static void insertDB(String eqp_code, String model_name, String zero_point) {
        Connection conn = getConnection();
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("insert into eqp_cam_model_info(eqp_code,model_name,zero_point) values(?,?,?)");
            pst.setString(1, eqp_code);
            pst.setString(2, model_name);
            pst.setString(3, zero_point);
            pst.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pst != null) {
                try {
                    pst.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");  //加载数据库驱动
            System.out.println("数据库驱动加载成功");
            String url = "jdbc:mysql://47.95.154.48:3306/hafy_db?useSSL=false";
            //如果不加useSSL=false就会有警告，由于jdbc和mysql版本不同，有一个连接安全问题
            String user = "root";
            String passWord = "H1a3F6y_";
            conn = (Connection) DriverManager.getConnection(url, user, passWord); //创建连接
            System.out.println("已成功的与数据库MySQL建立连接！！");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
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


    public static JSONObject readXls(File xlsFile) throws Exception {
        JSONObject datas = new JSONObject();
        Workbook wb = null;
        try {
            wb = WorkbookFactory.create(xlsFile);
            int numberOfSheets = wb.getNumberOfSheets();
            if (numberOfSheets > 0) {
                for (int i = 0; i < numberOfSheets; i++) {
                    Sheet sheet = wb.getSheetAt(i);
                    if (sheet != null) {
                        // sheet名字 rf lf rh rl
                        String sheetName = sheet.getSheetName();
                        JSONObject sheetNameJson = new JSONObject();
                        datas.put(sheetName, sheetNameJson);

                        // 获得当前sheet的结束行
                        int lastRowNum = sheet.getLastRowNum() + 1;
                        System.out.println("lastRowNum:" + lastRowNum);
                        boolean isXs = false;
                        String difValue = null;
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
                                                    if (value != null && !"".equals(value) && value.lastIndexOf("mm") > 0) {
                                                        difValue = value.substring(0, value.lastIndexOf("mm"));
                                                        JSONObject json = new JSONObject();
                                                        JSONArray array = new JSONArray();
                                                        json.put("mark", array);
                                                        sheetNameJson.put(difValue, json);
                                                    } else if ("xs".equals(value)) {
                                                        isXs = true;
                                                    }
                                                } else if (cellType.equals(CellType.NUMERIC)) {
                                                    if (isXs) {
                                                        // 系数
                                                        if (k == 1) {
                                                            // x的系数
                                                            double x = cell.getNumericCellValue();
                                                            System.out.println(difValue + ": x:" + x);

                                                            sheetNameJson.getJSONObject(difValue).put("xs_x", x);
                                                        } else if (k == 2) {
                                                            // y的系数
                                                            double y = cell.getNumericCellValue();
                                                            System.out.println(difValue + ": y:" + y);
                                                            sheetNameJson.getJSONObject(difValue).put("xs_y", y);
                                                            isXs = false;
                                                        }
                                                    } else {
                                                        // 具体值
                                                        JSONArray tmp = sheetNameJson.getJSONObject(difValue).getJSONArray("mark");
                                                        if (k == 0) {
                                                            int t = ((int) cell.getNumericCellValue());
                                                            if (tmp.size() < t) {
                                                                tmp.add(new JSONObject());
                                                            }
                                                        } else if (k == 1) {
                                                            JSONObject jsonObject = tmp.getJSONObject(tmp.size() - 1);
                                                            jsonObject.put("x", cell.getNumericCellValue());
                                                        } else if (k == 2) {
                                                            JSONObject jsonObject = tmp.getJSONObject(tmp.size() - 1);
                                                            jsonObject.put("y", cell.getNumericCellValue());
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
