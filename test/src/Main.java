import java.util.Scanner;
public class Main{
    public static void main(String [] args){
        Scanner input = new Scanner(System.in);
        System.out.println("Enter ur string");
        String strInput = input.nextLine();
        int cnt = 0;
        for(int i = 0; i < strInput.length(); i++){
            if(strInput.charAt(i) == 'b'){
                cnt++;
            }
        }
        System.out.println("символ \"b\" был найден"+cnt+"раз");
    }

}