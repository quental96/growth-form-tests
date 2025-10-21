import java.util.List;

//anaylses one line of text and provides individual parts
//reports "noop" for blank line or comment
public class Parser {

   private String commandWord;               //output of parser
   private int[] arg = new int[3];           //output of parser
   private BudType argT;                     //output of parser
   private boolean legal;                    //true if given legal input
   private String errorExplanation;
   
   //input is list of token words, list of legal commands, and list of BudTypes
   public Parser(String line, String[] commands, List<BudType> budList) {
      String cmd = line.trim().toLowerCase();
      int commentIndex = cmd.indexOf("/");     //strip inline comments
      if (commentIndex >= 0) cmd = cmd.substring(0, commentIndex);
      if (cmd.length() == 0 || wordColon(cmd)!=null) {  
         commandWord = "noop";            //comment or blank line or X:
         legal = true;
         return;
      }
      String[] word = cmd.split("\\s");      
      errorExplanation = "No error";
      String ans = null;
      String wd = word[0];  
      for (String w : commands)
         if (wd.equals(w.substring(1, Math.min(w.length(), wd.length()+1)))) 
            if (ans!=null) {error("Ambiguous input: " + wd); return;}   //two matches
            else ans = w;
      if (ans == null) {error("Unknown command: " + wd); return;}      //no matches
      if (ans.charAt(0)=='0' && word.length!=1) {error("Extra text after " + wd); return;} 
      if (ans.charAt(0)=='1' && word.length!=2) {error("1 int required after " + wd); return;} 
      if (ans.charAt(0)=='3' && word.length!=4) {error("3 ints required after " + wd); return;} 
      if (ans.charAt(0)=='T' && word.length!=2) {error("BudType required after " + wd);  return;}
      if (ans.charAt(0)=='U' && word.length!=3) {error("BudType and int required after " + wd);  return;}
      int offset = 0;
      if (ans.charAt(0)=='T' || ans.charAt(0)=='U'){ //cmd should have type afterwards
         for (BudType bt : budList)
            if (bt.matchName(word[1])) {
               argT = bt;
               commandWord = ans.substring(1);   //peel off flag character
               if (ans.charAt(0)=='T'){          //done if T
                  legal = true;
                  return;
               }
               offset = 1;                       //more to go if U
            }
         if (offset!=1) {error("Not a BudType: " + word[1]); return;}
      }
      
      int nArgs = word.length-1 - offset;           //cmd should have some ints after it
      for (int i=0; i<nArgs; i++){
         try {
            arg[i] = Integer.parseInt(word[i+1+offset]);
         } catch (NumberFormatException e) {
            {error("Not an integer: " + word[i+1+offset]); return;}
         }
      }
      commandWord = ans.substring(1);
      legal = true;
      return;
   }

   //if string is of the form "X:", return X, else return null
   public static String wordColon(String s){
      int commentIndex = s.indexOf("/");     //strip inline comments
      if (commentIndex >= 0) s = s.substring(0, commentIndex);
      String[] word = s.trim().split("\\s");
      if (word.length != 1) return null;
      if (word[0].length() < 2) return null;
      if (word[0].substring(word[0].length()-1).equals(":") && word[0].charAt(0)!='/')
            return word[0].substring(0, word[0].length()-1);
      return null;
      }
   
   private void error(String msg){
      errorExplanation=msg;
      legal=false;
   }
   
   public String getCommandWord() {
      return commandWord;
   }
   
   public boolean isLegal() {
      return legal;
   }
   
   public String getErrorExplanation() {
      return errorExplanation;
   }
   
   public int getArg1() {
      return arg[0];
   }
   
   public int getArg2() {
      return arg[1];
   }
   
   public int getArg3() {
      return arg[2];
   }
   
   public BudType getBudType() {
      return argT;
   }
   
   
 
}
