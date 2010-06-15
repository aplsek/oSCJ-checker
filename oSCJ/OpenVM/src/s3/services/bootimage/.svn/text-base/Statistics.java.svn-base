package s3.services.bootimage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import ovm.core.domain.Blueprint;
import ovm.core.domain.Type;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3Domain;
import s3.core.domain.S3Type;
import s3.util.Walkabout;
import ovm.core.domain.DomainDirectory;

/**
 *  Compute information about the bootimage. The function printStats performs
 *  the main work.
 * @author Jan Vitek
 */
public class Statistics extends Walkabout implements Ephemeral.Void {

    /** When true, class names are printed without packages which can be
     * ambiguous but is easier on the eyes.*/
    private static boolean shorten = true;
    private static final HashMap ifc_types_in_repository = new HashMap();
    private static final HashMap obj_types_in_repository = new HashMap();
    private HashMap types_of_objects_in_image = new HashMap();
    private HashMap types_of_arrays_in_image = new HashMap();
    private final S3Domain domain_;
  
    /** Create a new Statistics for a particular domain.  */
    private Statistics() {
        super(true, false);
        this.domain_ = (S3Domain) DomainDirectory.getExecutiveDomain();
        register(new ObjectHandler()); // the object handler is used by the 
    }                                 // walkabout while visiting the object graph.

    // Never add objects to the work list.
    protected boolean markAsSeen(Object _) { return false; }

  /**
   * This object advice is called for all objects in the image it stores
   * all arrays and objects as StatEntry
   * @author janvitek
   */
   private class ObjectHandler extends BootBase implements Walkabout.ObjectAdvice {

        public Class targetClass() {  return Object.class;  }

        public Object beforeObject( Object o) 
        {
            Class cl     =  o.getClass();
            String nm    =  cl.toString();
            HashMap map  =  cl.isArray() ? types_of_arrays_in_image : types_of_objects_in_image;
            StatEntry se =  (StatEntry) map.get( nm);
            if ( se == null ) map.put( nm, se = new StatEntry( nm));
            se.add( o);
            return null;
        }
    }
  
   private static PrintWriter out;
   
   private void summary() {
       

       try { out = new PrintWriter(new FileWriter(new File("stat.txt")));
       } catch (IOException e) { fail(e); }
       
       Object[] arr_types = types_of_arrays_in_image.values().toArray();
       Object[] obj_types = types_of_objects_in_image.values().toArray();
       int max_obj_cnt = 0, max_obj_sz  = 0;
       int max_arr_sz  = 0, max_arr_cnt = 0;
       
       for (int i = 0; i < obj_types.length; i++ ) {
           StatEntry se   = (StatEntry) obj_types[i];
           objects_count += se.number_of_instances;
           objects_size  += se.size_in_bytes;
           max_obj_cnt    = objects_count > max_obj_cnt ? objects_count : max_obj_cnt;
           max_obj_sz     = objects_size > max_obj_sz   ? objects_size  : max_obj_sz;
       }

       for (int i = 0; i < arr_types.length; i++ ) {
           StatEntry se  = (StatEntry) arr_types[i];
           arrays_count += se.number_of_instances;
           arrays_size  += se.size_in_bytes;
           max_arr_cnt   = arrays_count > max_arr_cnt ? arrays_count : max_obj_cnt;
           max_arr_sz    = arrays_size > max_arr_sz   ? arrays_size  : max_obj_sz;
       }
      
       Util.prln("\nOvm Bootimage statistics:");Util.line();
       Util.prln("The image contains " + (objects_count) +" objects and " + arrays_count + " arrays.");
       Util.prln("Total computed size for objects is " + (objects_size/1024)+" KB, and for array " +(arrays_size/1024)+" KB.");
       Util.prln("Instances of " + obj_types.length + " different object types inhabit the image");
       Util.prln("Instances of " + arr_types.length + " different array types inhabit the image");

       int gcmap =(objects_size + arrays_size + 64) / 32;
       Util.prln("The gcmap uses " + ((gcmap) / 1024) + " KB");
       Util.prln("For a computed size of " + ((objects_size+arrays_size + gcmap) / 1024) + " KB");
       
       Object[] rep_i_types = ifc_types_in_repository.values().toArray();
       Object[] rep_o_types = obj_types_in_repository.values().toArray();
       
       Util.prln("The repository contains "+ rep_o_types.length + " object types and " + rep_i_types.length + " array types." );
       Util.line(); Util.prln("Size in B | # Instances | Type ");
       Arrays.sort(obj_types);
       int osz = (max_obj_sz + "").length();
       int ocn = (max_obj_cnt+ "").length();
       for (int i = 0; i < obj_types.length; i++) {
           StatEntry se = (StatEntry) obj_types[i];
           Util.prln(Util.pad((se.size_in_bytes+""),osz) + " " 
                   + Util.pad((se.number_of_instances+""), ocn) + " " 
                   + Util.shorten(se.name));
       }
       Util.line(); Util.prln("Size in B | # Instances | Type | Percentage Full ");
       Arrays.sort(arr_types);
       int asz = (max_arr_sz + "").length();
       int acn = (max_arr_cnt+ "").length();
       for (int i = 0; i < arr_types.length; i++) {
           StatEntry se = (StatEntry) arr_types[i];
           String pcl = ((se.non_null_entries == -1) ? "" : " (" +
                            ((se.non_null_entries == 0) ? 0 : (se.non_null_entries * 100) / se.number_of_entries) + "%)");
           Util.prln(Util.pad((se.size_in_bytes+""),asz) + " " 
                   + Util.pad((se.number_of_instances+""), acn) + " " 
                   + Util.shorten(se.name) + pcl);
       }

       Util.to_stdout = false;
       
       Util.line();Util.prln("Classes in repository");
       Util.indent();
       String[] ln = new String[rep_o_types.length];
       for (int i = 0; i < rep_o_types.length; i++)
           ln[i] = ((S3Type)rep_o_types[i]).toShortString();
       Arrays.sort(ln);
       for (int i = 0; i < ln.length; i++)
           Util.prln(ln[i]);
       Util.outdent();
       printIfcs();
       
       out.close();      
   }
   
   /**
    * Remember all types appearing in the Repository.    */
   public static void registerBpForStatistics(Blueprint bp) {
 
        final S3Type type = (S3Type) bp.getType();

        if ( type.isInterface() ) {
            if ( ifc_types_in_repository.get( type) == null ) ifc_types_in_repository.put( type, new Vector(1));
        } else if ( type.isPrimitive() || type.isArray() ) {
        } else {
            obj_types_in_repository.put( type, type); // remember all types
            
            if ( type.getMode().isAbstract()) return; // Throwing out
            // abstract classes because, the output is meant to show only
            // interfaces used by classes which can actually be instanciated.
            Type.Interface[] interfaces = type.getAllInterfaces();

            for ( int i = 0; i < interfaces.length; i++ ) {
                Type.Interface o = interfaces[i];
                Vector v = (Vector) ifc_types_in_repository.get( o);
                if ( v == null ) ifc_types_in_repository.put( o, v = new Vector(2));                
                v.add( bp);
            }
        }
    }

   /**
    * Print a summary of all intefaces in the image and of their
    * implementing classes.
    */
    private static void printIfcs() {
        Iterator iter = ifc_types_in_repository.keySet().iterator();
        Vector zeros  = new Vector();
        Vector notzeros = new Vector();
        while ( iter.hasNext() ) {
            Object ifc = iter.next();
            Vector v = (Vector) ifc_types_in_repository.get(ifc);
            if (v.size() != 0)  notzeros.add(ifc);
            else                zeros.add(ifc.toString());
        }
        
        Object[] nzs = notzeros.toArray();
        Arrays.sort(nzs, new Comparator() {
            public int compare(Object x, Object y) {
                String s = x.toString(); String t = y.toString();
                int xl = ((Vector) ifc_types_in_repository.get(x)).size();
                int yl = ((Vector) ifc_types_in_repository.get(y)).size();
                if (xl == yl)  return s.compareTo(t);
                else           return yl - xl;
            }});

        for (int i = nzs.length - 1; i >= 0; i--) {
            S3Type ifc =  (S3Type) nzs[i];
            nzs[i] = ((Vector) ifc_types_in_repository.get( ifc)).size() + " "
                   + Util.shorten(ifc.toShortString());
        }

        Object[] zs = zeros.toArray();
        Arrays.sort(zs, new Comparator() {
            public int compare(Object x, Object y) {
                return x.toString().compareTo(y.toString());
            }});

        for (int i = zs.length - 1; i >= 0; i--) zs[i] = Util.shorten(zs[i].toString());
        boolean t = Util.to_stdout;
        Util.to_stdout = false;
        Util.prln("");Util.line();
        Util.prln("Implemented interfaces (not counting abstract classes): " + notzeros.size());
        Util.line();  Util.indent();
        for(int i=0;i<nzs.length;i++) Util.prln((String)nzs[i]);  Util.outdent(); Util.prln(""); Util.line();
        Util.prln("Unimplemented interfaces: " + zeros.size());
        Util.line();  Util.indent(); for(int i=0;i<zs.length;i++) Util.prln((String)zs[i]); 
        Util.outdent(); Util.to_stdout = t;
    }

    /**
     * Print the statistics for a particular driver, domain, and object set.
     */
    static public void printStats() {
        final Statistics stats = new Statistics();
	GC.the().walkLiveObjects(stats);
        stats.summary();
    }

    private int objects_count;
    private int arrays_count;
    private int objects_size;
    private int arrays_size;


    /** Helper class to remember a type name and an object count. 
     *  Works with arrays (where totalLenght is the sum of all
     *  array sizes) and non-array reference types. */
    private  class StatEntry implements Comparable {
       
        String name; // type name
        boolean is_array;
        String toString;
        int size_in_bytes;
        int number_of_instances;
        boolean found = true;
        int non_null_entries = -1;
        int number_of_entries;
        Object representative;
        Class class_;
        int fixedSize;
        int componentSize;
 
        StatEntry(String n)         {  name = n; }

        void add(Object o) {  
            if ( representative == null ) {
                representative = o;
                class_     = o.getClass();
                is_array   = class_.isArray();
                fixedSize  = getFixedSize(class_);
                if (is_array) componentSize = getComponentSize(class_);                   
            }
            number_of_instances++;
            size_in_bytes += fixedSize;
            
            if ( is_array) {
                size_in_bytes += getArrayLength( class_, o) * getComponentSize( class_);
                number_of_entries += getArrayLength( class_, o);
                if ( representative instanceof Object[] ) {
                    if (non_null_entries == -1) non_null_entries = 0;
                    Object[] arr = (Object[]) o;
                    for (int j = 0; j < arr.length; j++)
                            if (arr[j] != null)  non_null_entries++;
                }
            }                 
       }
                        
       int  getFixedSize(Class cl) {
            try {
                
                S3Blueprint bp = ReflectionSupport.blueprintFor(cl, domain_);
                return bp.getFixedSize();
            } catch (Throwable e) {
                found = false;
                Util.prln(""+e);
                return 0;
            }
        }
        int  getComponentSize(Class cl) {
            try {
                S3Blueprint.Array bp =
                    (S3Blueprint.Array) ReflectionSupport.blueprintFor(cl,domain_);
                return bp.getComponentSize();
            } catch (Throwable e) { } // Should be reported by getFixedSize
            return 0;
        }
         /** order elements by number of objects size first then by class name */
        public int compareTo( Object o) {
            StatEntry se = (StatEntry) o;
            int cmp = -size_in_bytes + se.size_in_bytes;
            return ( cmp == 0 ) ? name.compareTo( se.name) : cmp;
        }
    } 
    

     static private int getArrayLength(Class objClass, Object object) {
        int length = 0;
        if (objClass == byte[].class)        length = ((byte[]) object).length;
        else if (objClass == int[].class)    length = ((int[]) object).length;
        else if (objClass == boolean[].class)length = ((boolean[]) object).length;
        else if (objClass == short[].class)  length = ((short[]) object).length;
        else if (objClass == char[].class)   length = ((char[]) object).length;
        else if (objClass == float[].class)  length = ((float[]) object).length;
        else if (objClass == long[].class)   length = ((long[]) object).length;
        else if (objClass == double[].class) length = ((double[]) object).length;
        else if (object instanceof Object[]) length = ((Object[]) object).length;
        return length;
    }

    static private  class Util {
        static boolean to_stdout = true;
        static int pad = 0;
        static String padString = "";
        static void indent() {
            pad +=4;
            padString += "    ";
        }
        static void outdent() {
            pad -= 4;
            padString = "";
            for (int i=0;i<pad;i++) padString += " ";
        }
        static  String shorten(String s) {
            if (!shorten) return s;
    
            String result = "";
            String leading = "";
            if (s.startsWith("class ")) s = s.substring(6);
            while (s.startsWith("[")) { leading += '['; s = s.substring(1); }
    
            int lastsemi = s.lastIndexOf(';');
            lastsemi = (lastsemi < s.length() - 1) ? s.length() : lastsemi;
            s = s.substring(0, lastsemi);
            if (s.lastIndexOf('/') > 0) {
                int lastslash = s.lastIndexOf('/') + 1;
                lastslash = (lastslash < 0) ? 0 : lastslash;
                result = s.substring(lastslash, s.length());
            } else {
                int lastdot = s.lastIndexOf('.') + 1;
                lastdot = (lastdot < 0) ? 0 : lastdot;
                result = s.substring(lastdot, s.length());
            }
            return leading + result;
        }
        
        /**
         * pad a string to paddelength by prefixing spaces
         */
        static public String pad(String s, int paddedlength) {
            if (s.length() >= paddedlength)
                return s;
            StringBuffer r = new StringBuffer(paddedlength);
            int i = 0;
            while (i < paddedlength - s.length())
                r.insert(i++, ' ');
            r.insert(i, s);
            return r.toString();
        }
        /**
         * pad a string to paddelength by appending spaces
         */
        static public String padAtEnd(String s, int paddedlength) {
            if (s.length() >= paddedlength)
                return s;
            StringBuffer r = new StringBuffer(paddedlength);
            int i = 0;
            while (i < paddedlength - s.length())
                r.insert(i++, ' ');
            r.insert(0, s);
            return r.toString();
        }
        private static void pr(String ss)   {
            String s = padString + ss;
            if (to_stdout) System.err.print(s);
            if (out != null)   out.print(s);
        }
        private static void prln(String ss) {  
            String s = padString + ss;
            if (to_stdout) System.err.println(s); 
            if (out != null) out.print(s+"\n");
        }
        
        private static void line() {  for (int i = 0; i < 40; i++) pr("-"); prln(""); }
   
    }
} 
