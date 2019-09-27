import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DumpModel2Csv {

    private static String basePath = new File(SummaryMain.class.getClassLoader().getResource("").getPath()).getParent();

    private static String csvPath = basePath + "/csv";

    public static void main(String[] args) {
        // 读取所有模板数据
        JSONArray models = select();

        String fileName = "model_h_x.csv";
        String csvFilename = csvPath + "/" + fileName;
        File path = new File(csvPath);
        if (!path.exists()) {
            path.mkdirs();
        }
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(csvFilename), "UTF-8");
            String title = "id,name,gender,height,weight,shoe_size,model_name,lh_x1,lh_x5,lh_x9,rh_x1,rh_x5,rh_x9\n";
            pw.write(title);
            for (int i = 0; i < models.size(); i++) {
                JSONObject jsonObject = models.getJSONObject(i);
                StringBuilder data = new StringBuilder();
                data.append(jsonObject.getIntValue("model_id")).append(",");
                data.append(jsonObject.getString("name")).append(",");
                data.append(jsonObject.getIntValue("gender")).append(",");
                data.append(jsonObject.getIntValue("height")).append(",");
                data.append(jsonObject.getIntValue("weight")).append(",");
                data.append(jsonObject.getIntValue("shoe_size")).append(",");
                data.append(jsonObject.getString("model_name")).append(",");

                JSONObject acupoint_points = jsonObject.getJSONObject("acupoint_points");
                // 左手
                JSONArray lh = acupoint_points.getJSONArray("lh");
                fillX(lh, data);

                // 右手
                JSONArray rh = acupoint_points.getJSONArray("rh");
                fillX(rh, data);

                pw.write(data.toString() + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    private static void fillX(JSONArray hand, StringBuilder data) {
        JSONObject jsonObject1 = hand.getJSONObject(0);
        data.append(jsonObject1.getDoubleValue("x")).append(",");
        JSONObject jsonObject5 = hand.getJSONObject(4);
        data.append(jsonObject5.getDoubleValue("x")).append(",");
        JSONObject jsonObject9 = hand.getJSONObject(8);
        data.append(jsonObject9.getDoubleValue("x")).append(",");
    }

    private static JSONArray select() {
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet resultSet = null;
        JSONArray datas = new JSONArray();
        try {
            conn = getConnection();
            pst = conn.prepareStatement("select * from acupoint_model order by model_id");
            resultSet = pst.executeQuery();
            while (resultSet.next()) {
                JSONObject data = new JSONObject();
                data.put("model_id", resultSet.getInt("model_id"));
                String model_name = resultSet.getString("model_name");
                String acupoint_points = resultSet.getString("acupoint_points");
                data.put("name", resultSet.getString("name"));
                data.put("gender", resultSet.getInt("gender"));
                data.put("height", resultSet.getInt("height"));
                data.put("weight", resultSet.getInt("weight"));
                data.put("shoe_size", resultSet.getInt("shoe_size"));
                data.put("model_name", model_name);
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

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");  //加载数据库驱动
            //System.out.println("数据库驱动加载成功");
            String url = "jdbc:mysql://47.95.154.48:3306/hafy_db?useSSL=false";
            //如果不加useSSL=false就会有警告，由于jdbc和mysql版本不同，有一个连接安全问题
            String user = "root";
            String passWord = "H1a3F6y_";
            conn = (Connection) DriverManager.getConnection(url, user, passWord); //创建连接
            //System.out.println("已成功的与数据库MySQL建立连接！！");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }


}
