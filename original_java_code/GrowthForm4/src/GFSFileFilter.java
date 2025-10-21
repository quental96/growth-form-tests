import java.io.File;
import javax.swing.filechooser.FileFilter;


public class GFSFileFilter extends FileFilter {
   public boolean accept(File f) {
      if (f.isDirectory()) return true;
      String extension = f.getName();
      if (extension == null) return false;
      extension = ("xxxx" + extension).substring(extension.length());
      if (extension.equals(".gfs") || extension.equals(".GFS")) return true;
      return false;
  }

  //The description of this filter
  public String getDescription() {
      return "Growth/Form script file";
  }
}
