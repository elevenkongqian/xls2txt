public class Test {
    public static void main(String[] args) {
        String value = "0mm";
        int difValue = Integer.parseInt(value.substring(0, value.lastIndexOf("mm")));
        int t =4;
        System.out.println(roundHalfUp(t));
    }

    public static int roundHalfUp(int t){
        return t%10>=5?t/10*10+10:t/10*10;
    }
}
