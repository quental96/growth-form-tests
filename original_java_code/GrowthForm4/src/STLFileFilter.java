import java.io.File;
import javax.swing.filechooser.FileFilter;

// Accept all directories but only STL files.
class STLFileFilter extends FileFilter {

  public boolean accept(File f) {
      if (f.isDirectory()) return true;
      String extension = f.getName();
      if (extension == null) return false;
      extension = ("xxxx" + extension).substring(extension.length());
      if (extension.equals(".stl") || extension.equals(".STL")) return true;
      return false;
  }

  //The description of this filter
  public String getDescription() {
      return "STL 3D object";
  }
}