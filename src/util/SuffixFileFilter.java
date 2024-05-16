package util;

import java.io.FilenameFilter;
import java.io.File;

public class SuffixFileFilter implements FilenameFilter {

  String suffix = null;

  public SuffixFileFilter(String suffix) {
    this.suffix = suffix;
  }

  public boolean accept(File dir, String name) {
    return name.endsWith(suffix);
  }

}
