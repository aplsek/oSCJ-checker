public class StandardProperties {

static String[] props = {
    "java.version", // Java Runtime Environment version 
    "java.vendor", //  Java Runtime Environment vendor 
    "java.vendor.url", // Java vendor URL 
    "java.home", // Java installation directory 
    "java.vm.specification.version", // Java Virtual Machine specification version 
    "java.vm.specification.vendor", // Java Virtual Machine specification vendor 
    "java.vm.specification.name", // Java Virtual Machine specification name 
    "java.vm.version", // Java Virtual Machine implementation version 
    "java.vm.vendor", // Java Virtual Machine implementation vendor 
    "java.vm.name", // Java Virtual Machine implementation name 
    "java.specification.version", // Java Runtime Environment specification version 
    "java.specification.vendor", // Java Runtime Environment specification vendor 
    "java.specification.name", // Java Runtime Environment specification name 
    "java.class.version", // Java class format version number 
    "java.class.path", // Java class path 
    "java.library.path", // Path for finding Java libraries
    "java.io.tmpdir", //Default temp file path
    "java.compiler", // Name of JIT to use
    "java.ext.dirs", // Path of extension directory or directories 
    "os.name", // Operating system name 
    "os.arch", // Operating system architecture 
    "os.version", // Operating system version 
    "file.separator", // File separator ("/" on UNIX) 
    "path.separator", // Path separator (":" on UNIX) 
    "line.separator", // Line separator ("\n" on UNIX) 
    "user.name", // User's account name 
    "user.home", // User's home directory 
    "user.dir", // User's current working directory
    "file.encoding", // default platform character set encoding
    "user.timezone", // platform timezone description string
};

    public static void main(String[] args) {
        System.out.println("# Standard System Properties");
        for (int i = 0; i < props.length; i++) {
            String curProp = System.getProperty(props[i]);
            if (curProp == null)
                curProp = "<NOT SET>";
            StringBuffer modifiedProp = new StringBuffer(30);
            for (int j = 0; j < curProp.length(); j++) {
                int ch = curProp.charAt(j);
                if (ch == '\n')
                    modifiedProp.append("\\n");
                else if (ch == '\r')
                    modifiedProp.append("\\r");
                else
                     modifiedProp.append( (char)ch);
            }
     //     sprops.setProperty(props[i], modifiedProp);
            System.out.println(props[i] + "=" + modifiedProp );
        }
      //  sprops.store(System.out, "Standard System Properties");
    }

}
