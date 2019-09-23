import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class Model2DB {

    // 基础目录
    private static String basePath = new File(SummaryMain.class.getClassLoader().getResource("").getPath()).getParent();

    // 模型所在文件
    private static String modelPath = basePath + "/model";

    // 插入SQL
    private static final String INSTER_SQL = "insert into acupoint_model(`name`,height,weight,shoe_size,model_name,acupoint_points) values (?,?,?,?,?,?)";

    public static void main(String[] args) throws Exception {
        ArrayList<File> modelFiles = getModelFiles(modelPath);
        Connection conn = getConnection();
        try {
            // 清库(注意这个会清库 truncate)
            truncateDB(conn);
            for (File file : modelFiles) {
                String fileName = file.getName();
                //System.out.println(file.getName());
                // 模型名称
                String model_name = fileName.substring(0, fileName.lastIndexOf("."));
                //System.out.println(model_name);
                // 鞋码
                int shoe_size = Integer.parseInt(fileName.substring(0, 2));
                //System.out.println(shoe_size);
                String tmp1 = fileName.substring(fileName.length() - 10, fileName.length() - 4);
                String tmp2 = tmp1.substring(0, 1);
                //System.out.println(tmp2);
                if (!isNum(tmp2))
                    tmp1 = fileName.substring(fileName.length() - 9, fileName.length() - 4);
                // 名字(拼音)
                String name = fileName.substring(2, fileName.lastIndexOf(tmp1));
                //System.out.println(name);
                // 身高
                int height = Integer.parseInt(tmp1.substring(0, 3));
                //System.out.println(height);
                // 体重
                int weight = Integer.parseInt(tmp1.substring(3, tmp1.length()));
                //System.out.println(weight);
                // 穴位点坐标
                String acupoint_points = readModelFile(file);
                //System.out.println(acupoint_points);

                // 保存数据库
                insertDB(conn, name, height, weight, shoe_size, model_name, acupoint_points);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        System.out.println("执行完成!");
    }

    private static void truncateDB(Connection conn) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("truncate table acupoint_model");
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
        }
    }

    private static void insertDB(Connection conn, String name, int height, int weight, int shoe_size, String model_name, String acupoint_points) {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement(INSTER_SQL);
            // `name`,height,weight,shoe_size,model_name,acupoint_points
            pst.setString(1, name);
            pst.setInt(2, height);
            pst.setInt(3, weight);
            pst.setInt(4, shoe_size);
            pst.setString(5, model_name);
            pst.setString(6, acupoint_points);
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
        }
    }

    private static Connection getConnection() {
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


    private static String readModelFile(File file) {
        // 顺序：右脚 左脚 左手 右手
        BufferedReader br = null;
        JSONObject pointData = new JSONObject();
        JSONArray rfData = new JSONArray();
        JSONArray lfData = new JSONArray();
        JSONArray lhData = new JSONArray();
        JSONArray rhData = new JSONArray();
        pointData.put("rf", rfData);
        pointData.put("lf", lfData);
        pointData.put("lh", lhData);
        pointData.put("rh", rhData);
        try {
            br = new BufferedReader(new FileReader(file));
            String record = null;
            int rownum = 1;
            while ((record = br.readLine()) != null) {
                if (record.trim().equals("")) {
                    continue;
                }
                System.out.println(file.getName());
                JSONObject point = new JSONObject();
                String[] pointArray = record.split(" ");
                point.put("x", Double.parseDouble(pointArray[0]));
                point.put("y", Double.parseDouble(pointArray[1]));
                point.put("z", Double.parseDouble(pointArray[2]));
                point.put("rx", Integer.parseInt(pointArray[3]));
                point.put("ry", Integer.parseInt(pointArray[4]));
                point.put("rz", Integer.parseInt(pointArray[5]));

                if (rownum >= 1 && rownum <= 10) {
                    // 右脚 rf
                    rfData.add(point);
                } else if (rownum > 10 && rownum <= 20) {
                    // 左脚 lf
                    lfData.add(point);
                } else if (rownum > 20 && rownum <= 30) {
                    // 左手 lh
                    lhData.add(point);
                } else if (rownum > 30 && rownum <= 40) {
                    // 右手 rh
                    rhData.add(point);
                }
                rownum = rownum + 1;
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

        // 修复数据
        fix(pointData);
        return pointData.toJSONString();
    }

    private static void fix(JSONObject pointData) {
        fixValue("lh", 3, "x", 0.0D, pointData);
        fixValue("lh", 5, "x", 0.0D, pointData);
        fixValue("rh", 3, "x", 0.0D, pointData);
        fixValue("rh", 5, "x", 0.0D, pointData);

        fixLimitValue("lh", 6, "z", 0.247D, pointData);
        fixLimitValue("rh", 6, "z", 0.247D, pointData);
    }

    private static void fixLimitValue(String type, int index, String flag, double value, JSONObject pointData) {
        JSONArray h = pointData.getJSONArray(type);
        JSONObject point = h.getJSONObject(index);
        if (point.getDouble(flag) < value) {
            point.put(flag, value);
        }
    }

    private static void fixValue(String type, int index, String flag, double value, JSONObject pointData) {
        JSONArray h = pointData.getJSONArray(type);
        JSONObject point = h.getJSONObject(index);
        point.put(flag, value);
    }

    private static boolean isNum(String src) {
        try {
            Integer.parseInt(src);
            return true;
        } catch (Exception e) {
        }
        return false;
    }


    private static ArrayList<File> getModelFiles(String path) throws Exception {
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
