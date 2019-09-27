import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FixModel2DB {
    public static void main(String[] args) {
        // 读取所有模板数据
        JSONArray models = select();

        for (int i = 0; i < models.size(); i++) {
            JSONObject jsonObject = models.getJSONObject(i);
            //System.out.println("i:" + i + ",jsonObject:" + jsonObject);
            JSONObject acupoint_points = jsonObject.getJSONObject("acupoint_points");
            String model_name = jsonObject.getString("model_name");
            // 右手
            JSONArray rh = acupoint_points.getJSONArray("rh");
            boolean isChangedrh = fixData(rh);
            if(isChangedrh){
                System.out.println("model_name:" + model_name + ",rh:" + rh);
            }

            // 左手
            JSONArray lh = acupoint_points.getJSONArray("lh");
            boolean isChangedlh = fixData(lh);
            if(isChangedlh){
                System.out.println("model_name:" + model_name + ",lh:" + lh);
            }

            // 根据模板名更新模板表
            if(isChangedrh || isChangedlh){
                System.err.println("update model_name:" + model_name);
                update(model_name, acupoint_points);
            }
        }
    }

    private static void update(String model_name, JSONObject acupoint_points) {
        Connection conn = getConnection();
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("update acupoint_model set acupoint_points=? where model_name=?");
            pst.setString(1, acupoint_points.toJSONString());
            pst.setString(2, model_name);
            int updateNum = pst.executeUpdate();
            System.out.println("model_name:" + model_name + ", update num:" + updateNum);
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

    /**
     * 左右手Z值：2号点范围0.261～0.270，7号点范围0.250～0.252，10号点范围0.250～0.257
     *
     * @param hand
     */
    private static boolean fixData(JSONArray hand) {
        boolean isChanged = false;
        //System.out.println("hand:" + hand);
        // 2号点范围0.261～0.270
        JSONObject jsonObject1 = hand.getJSONObject(1);
        double z1 = jsonObject1.getDoubleValue("z");
        if (z1 < 0.261) {
            isChanged = true;
            jsonObject1.put("z", 0.261);
        } else if (z1 > 0.270) {
            isChanged = true;
            jsonObject1.put("z", 0.270);
        }

        // 7号点范围0.250～0.252
        JSONObject jsonObject6 = hand.getJSONObject(6);
        double z6 = jsonObject6.getDoubleValue("z");
        if (z6 < 0.250) {
            isChanged = true;
            jsonObject6.put("z", 0.250);
        } else if (z6 > 0.252) {
            isChanged = true;
            jsonObject6.put("z", 0.252);
        }

        // 10号点范围0.250～0.257
        JSONObject jsonObject9 = hand.getJSONObject(9);
        double z9 = jsonObject9.getDoubleValue("z");
        if (z9 < 0.250) {
            isChanged = true;
            jsonObject9.put("z", 0.250);
        } else if (z9 > 0.257) {
            isChanged = true;
            jsonObject9.put("z", 0.257);
        }
        return  isChanged;
    }

    private static JSONArray select() {
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet resultSet = null;
        JSONArray datas = new JSONArray();
        try {
            conn = getConnection();
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
