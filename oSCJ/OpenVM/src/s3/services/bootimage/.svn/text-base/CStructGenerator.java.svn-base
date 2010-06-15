package s3.services.bootimage;
import java.io.PrintWriter;

import ovm.core.domain.Field;
import ovm.core.domain.LinkageException;
import ovm.core.domain.ObjectModel;
import ovm.core.domain.Type;
import ovm.core.repository.Selector;
import ovm.core.services.format.CxxFormat;
import ovm.core.services.memory.MemoryManager;
import ovm.util.HashSet;
import ovm.util.Iterator;
import ovm.util.Set;
import s3.core.domain.S3Blueprint;
import s3.core.domain.S3Domain;
import java.io.IOException;

/*
Generates C representations of Java types. This is a modified copy of
CxxStructGenerator, which generated C++ code. The CxxStructGenerator can be
removed eventually.
*/

public class CStructGenerator extends CxxFormat {

    public static final boolean SUPPORT_LAYOUT_CHECKS = true;
    
    private PrintWriter w;
    private PrintWriter dw; /* debug symbol table writer */
    private int headerSize;

    /**
     * Generate the header file.
     * @param fname output file name
     * @param ds    the set of domains in the virtual machine
     **/
    static public void output(String fname, DomainSprout[] ds)
	throws IOException
    {
      output(fname, null, ds);
    }

    static public void output(String mainFileName, String debugSymbolTableFileName, DomainSprout[] ds) 
      throws IOException {
      
        PrintWriter dfw = null;
        
        if (debugSymbolTableFileName != null) {
          dfw = new PrintWriter(debugSymbolTableFileName);
        }
      	CStructGenerator gen = new CStructGenerator(new PrintWriter(mainFileName), dfw);

	BootImage.the().writeHeadersToC_h_File(gen.w);
	gen.genHeaderPrologue();
	gen.genDebugSymbolTablePrologue();

	// define object types for every domain
	// and also put them into the debug symbol table
	
	for (int i = 0; i < ds.length; i++)
	    gen.genStructs((S3Domain) ds[i].dom, ds[i].anal);

	gen.w.close();

	gen.genDebugSymbolTableEpilogue();
	
	
	if (dfw != null) {
	  dfw.close();
        }
	
    }

    private void genDebugSymbolTablePrologue() {
    
      if (dw == null) {
        return ;
      }
      
      dw.println("void pbp( HEADER *hdr ) {");
      dw.println("\tprintf(\"%x\\n\", HEADER_BLUEPRINT(hdr));");
      if (MemoryManager.the().usesArraylets()) {
        dw.println("\tprintf(\"%s\\n\", *(char **)(((struct e_s3_core_domain_S3Blueprint *)HEADER_BLUEPRINT(hdr))->ovm_dbg_1string->values));");      
      } else {
        dw.println("\tprintf(\"%s\\n\", ((struct e_s3_core_domain_S3Blueprint *)HEADER_BLUEPRINT(hdr))->ovm_dbg_1string->values);");
      }
      dw.println("}");


      dw.println("void print_nbytes( char *str, int count ) {\n");
      dw.println("\twhile(count--) { putchar(*str); str++; }");
      dw.println("}");

      dw.println("void print_nbytes_unicode( char *str, int count ) {\n");
      dw.println("\twhile(count--) { putchar(*str); str+=2; }");
      dw.println("}");
      
      dw.println("void print_estring( HEADER *hdr ) {");
      dw.println("\te_java_lang_String *str = (e_java_lang_String *) hdr;");
      if (MemoryManager.the().usesArraylets()) {
        dw.println("\tprint_nbytes((*(char **)(str->ovm_data->values)) + str->ovm_offset,str->ovm_count);");
      } else {
        dw.println("\tprint_nbytes( (char *)(str->ovm_data->values) + str->ovm_offset,str->ovm_count);");      
      }
      dw.println("}");
      
      dw.println("void print_ustring( HEADER *hdr ) {");
      dw.println("\tu1_java_lang_String *str = (u1_java_lang_String *) hdr;");
      if (MemoryManager.the().usesArraylets()) {
        dw.println("\tprint_nbytes_unicode((*(char **)(str->ovm_value->values)) + str->ovm_offset*2, str->ovm_count);");
      } else {
        dw.println("\tprint_nbytes_unicode( (char *)(str->ovm_value->values) + str->ovm_offset*2,str->ovm_count);");            
      }
      dw.println("}");
      
      //dw.println("void print_ustring_at_address( void * ptr ) { print_ustring(ptr); }");      
      
      dw.println("/* this is only for debugging, makes sure all types exist in symbol table */");
      dw.println("/* requires jtypes.h, intended to be included into ovm_inline.c */");
      dw.println("");
      dw.println("/* the function is never called */");
      dw.println("");
      

      dw.println("void dummy_debugSymbolTableGenerator(void) {");
      
     // FIXME: 
     // dw.println("\tstruct s3_core_domain_S3Blueprint  _dummy_struct_s3_core_domain_S3Blueprint;");
     // dw.println("\ts3_core_domain_S3Blueprint  _dummy_s3_core_domain_S3Blueprint;");
      
    }
    
    private void genDebugSymbolTableEpilogue() {
    
      if (dw == null) {
        return ;
      }
      
      dw.println("}");
      dw.println("");
    }
    
    private void genHeaderPrologue() {
    
        ObjectModel m = ObjectModel.getObjectModel();
        w.println(m.toCxxStruct());
        w.println("typedef struct HEADER HEADER;");
        headerSize = m.headerSkipBytes();
    }

    private void genStructDecls(S3Domain dom, Analysis anal) {
      //  S3Blueprint root = null;  unused
        for (Iterator it = dom.getBlueprintIterator(); it.hasNext();) {
            S3Blueprint bp = (S3Blueprint) it.next();
            if (bp.getType().isClass() && !bp.getType().isSharedState()) {
		if (anal.shouldCompile(bp)) {
		    w.print("struct ");
		    w.print(format(bp));
		    w.println(";");
		    // if (bp.getParentBlueprint() == null)
		    //    root = bp;
		    w.print("typedef struct ");
		    w.print(format(bp));
		    w.print(" ");
		    w.print(format(bp));
		    w.println(";");
		} else {
		    w.print("typedef HEADER ");
		    w.print(format(bp));
		    w.println(";");
		}
	    } 
	    if (bp.getType().isScalar() && bp.getType().isSharedState()) {
	    	    w.print("typedef struct ");
		    w.print(format(bp));
		    w.print(" ");
		    w.print(format(bp));
		    w.println(";");
	    } 
        }
        w.println("");

        // See FIXME in genStructDefs for some thoughts on this
        // maneuver. Be careful to typedef interfaces after
        // <d>_java_lang_Object has been defined.
        for (Iterator it = dom.getBlueprintIterator(); it.hasNext();) {
            S3Blueprint bp = (S3Blueprint) it.next();
            if (bp.getType().isInterface()) {
                S3Blueprint zuper = bp.getParentBlueprint();
                w.println(
                    "typedef struct " + format(zuper) + " " + format(bp) + ";");
            }
        }
        w.println("");
    }

    private void dump(S3Domain dom, Analysis anal, Set seen, S3Blueprint bp) {

      dump(dom,anal,seen,bp,false,null);
    }

    private void dump(S3Domain dom, Analysis anal, Set seen, S3Blueprint bp, boolean membersOnly, S3Blueprint masterBP) {

	S3Blueprint.Scalar zuper = bp.getParentBlueprint();
	if (zuper != null)
	    genStructDef(dom, anal, seen, zuper);

	// FIXME: Interfaces are typedeffed above. This pretty
	// much ties gcc's hands for type-based alias analysis.
	// Should use virtual inheritence? How would that affect
	// object layout?
	if (bp.getType() instanceof Type.Class) {
          
          Field[] fsel = bp.getLayout();
          
            // now dump the structure for the class itself
            
            if (!membersOnly) {
            
              if (dw != null) {
                dw.print("\tstruct ");
                dw.print(format(bp));
                dw.print(" _dummy_struct_");
                dw.print(format(bp));
                dw.println(";");
                
                dw.print("\t ");
                dw.print(format(bp));
                dw.print(" _dummy_");
                dw.print(format(bp));
                dw.println(";");                
              }
            
    	      w.print("struct ");
	      w.print(format(bp));
	      w.println(" {");
            }
            
            
	    if (zuper == null) {
	      /* dump HEADER members */
	      w.print("    char _HEADER_space[");
	      w.print(headerSize);
	      w.println("];");
	      if (SUPPORT_LAYOUT_CHECKS) {
	        w.println("/* CHK " + format(bp) + " _HEADER_space */");
              }
	      w.println("    /* end of superclass HEADER */");	      
	      
            } else {
                dump(dom,anal,seen,zuper,true,bp);
            

            }
            
	    fsel = bp.getLayout();
	    if (membersOnly) {
              w.print("    /* end of superclass ");
              w.print(format(bp));
              w.println("*/");
            }
	    for (int i =
		     (zuper == null ? headerSize : zuper.getUnpaddedFixedSize()) / 4;
		 i < fsel.length;
		 i++) {
		if (fsel[i] == null)
		    // prev (exclusive) or next wide
		    continue;
		Field f = fsel[i];
		S3Blueprint fbp;
                
		try {
		    try {
			fbp = (S3Blueprint) dom.blueprintFor(f.getType());
		    } catch (LinkageException e) {
			// the LinkageException.Runtime we
			// care about is thrown by f.getType
			throw e.unchecked();
		    }
		} catch (LinkageException.Runtime e) {
		    String name = fsel[i].getSelector().getName();
		    BootBase.d(
			       BootBase.LOG_INCOMPLETE_TYPES,
			       "can't find type of "
			       + format(bp)
			       + "."
			       + name
			       + "(descriptor = "
			       + fsel[i].getSelector().getDescriptor()
			       + ") using Oop");
		    w.print("    HEADER *");
		    w.print(encode(name));
		    if (membersOnly) {
		      w.print("_");
		      w.print(format(bp));
                    }

		    w.print(";\t// Really ");
		    w.println(fsel[i].getSelector().getDescriptor());
		    
                    if (SUPPORT_LAYOUT_CHECKS) {
                      if (membersOnly) {
                        w.println("/* CHK " + format(masterBP) + " " + encode(name) + "_" + format(bp)+ " */");
                      } else {
                        w.println("/* CHK " + format(bp) + " " + encode(name) + " */");
                      }
                    }
		    
		    continue;
		}
		String name = encode(fsel[i].getSelector().getName());
		w.print("    ");
		
                // w.print("/* selector: "+fsel[i].getSelector()+" */");
		if (fbp.isScalar()) {
		  w.print("HEADER *");
		  w.print(name);
		  if (membersOnly) {
		      w.print("_");
		      w.print(format(bp));
                    }
		  
		  w.print(";\t// Real reference type is ");
		  w.println(fsel[i].getSelector().getDescriptor());
		  
		  if (SUPPORT_LAYOUT_CHECKS) {
                    if (membersOnly) {
                      w.println("/* CHK " + format(masterBP) + " " + name + "_" + format(bp)+ " */");
                    } else {
                      w.println("/* CHK " + format(bp) + " " + name + " */");
                    }
                  }

                  
		} else {
  		  if (fbp instanceof S3Blueprint.Primitive &&
		        fbp.getUnpaddedFixedSize() > s3.core.domain.MachineSizes.BYTES_IN_WORD)
                        w.print("__attribute__((aligned(8))) ");
		  w.print(format(fbp.promote()));
		  w.print(fbp.isReference() ? " *" : " ");
		  w.print(name);
		  if (membersOnly) {
		      w.print("_");
		      w.print(format(bp));
                    }
		  
		  w.print(";");
                    
		  if (fbp instanceof S3Blueprint.Array) {
		    w.print(" /* "+ format(((S3Blueprint.Array)fbp).getComponentBlueprint()) + " */");
                  }
                  w.println("");
                  
                  if (SUPPORT_LAYOUT_CHECKS) {
                    if (membersOnly) {
                      w.println("/* CHK " + format(masterBP) + " " + name + "_" + format(bp)+ " */");
                    } else {
                      w.println("/* CHK " + format(bp) + " " + name + " */");
                    }
                  }
                }
	    }

          if (!membersOnly) {
  	      w.println("};");
          }
        }
        
	if (!membersOnly && (bp == dom.ROOT_BLUEPRINT)) {
	    // As soon as Object is defined, we can define array
	    // types for our domain
	    w.println("struct " + format(dom) + "_Array { ");
	    w.println("    "+format(bp) + " _parent_;");
            if (SUPPORT_LAYOUT_CHECKS) {
    	      w.println("/* CHK " + format(dom) + "_Array _parent_ */" );
            }
	    
	    w.println("    int length;");
	    if (SUPPORT_LAYOUT_CHECKS) {
              w.println("/* CHK " + format(dom) + "_Array length */" );	    
            }
	    
//	logical value would be 0 - and it would probably work, but the C++ version had
//	there "1" ; if you are brave enough, go to 0 :)	    
//	    w.println("    char values[0];");
	    w.println("    char values[1];");
	    if (SUPPORT_LAYOUT_CHECKS) {
	      w.println("/* CHK " + format(dom) + "_Array values */" );
            }
	    
	    w.println("};");
	    w.println("typedef struct "+ format(dom) + "_Array " + format(dom) + "_Array;"); 
	    w.println("");

	    w.println("struct " + format(dom) + "_Array_8al { ");
	    w.println("    "+format(bp) + " _parent_;");
	    if (SUPPORT_LAYOUT_CHECKS) {
	      w.println("/* CHK " + format(dom) + "_Array_8al _parent_ */" );	    
            }
	    
	    w.println("    int length;");
            if (SUPPORT_LAYOUT_CHECKS) {	    
	      w.println("/* CHK " + format(dom) + "_Array_8al length */" );	    	    
            }

//	logical value would be 0 - and it would probably work, but the C++ version had
//	there "1" ; if you are brave enough, go to 0 :)	    	    
//	    w.println("    __attribute__((aligned(8))) char values[0];");
	    w.println("    __attribute__((aligned(8))) char values[1];");
            if (SUPPORT_LAYOUT_CHECKS) {
	      w.println("/* CHK " + format(dom) + "_Array_8al values */" );
	    }	    
	    
	    w.println("};");
	    w.println("typedef struct "+ format(dom) + "_Array_8al " + format(dom) + "_Array_8al;"); 
	    w.println("");
/*	    	    
	    w.println("struct " + format(dom) + "_Array_dims { ");
	    w.println("    "+format(bp) + " _parent_;");
	    w.println("    int length;");
	    w.println("    char values[sizeof(jint)*2];");
	    w.println("};");
	    w.println("typedef struct "+ format(dom) + "_Array_dims " + format(dom) + "_Array_dims;"); 
	    w.println("");	    
*/
	}
    }

    private void specializeArrayTemplate(S3Domain dom,String s) {
         w.print("template <int sz> struct "+ format(dom)
             + "_Array<"+s+",sz>: ");
         w.println("public " + format(dom) + "_ARRAY {");
         w.println("    __attribute__((aligned(8))) "+s+" values[sz];");
         w.println("};");
         w.println("");
    }

    private void genStructDef(S3Domain dom, Analysis anal,
			      Set seen, S3Blueprint bp) {
        if (seen.contains(bp))
            return;
        seen.add(bp);
        if (anal.shouldCompile(bp)) {
        
            if (false) {
              w.println("// FIXED SIZE OF "+bp+" is "+bp.getFixedSize());
            }
	    dump(dom, anal, seen, bp);
	    dump(dom, anal, seen,
		 (S3Blueprint) bp.getSharedState().getBlueprint());
	}
    }

    private void genStructDefs(S3Domain dom, Analysis anal) {
        Set seen = new HashSet();
        for (Iterator it = dom.getBlueprintIterator(); it.hasNext();) {
            S3Blueprint bp = (S3Blueprint) it.next();
            if (!bp.isSharedState())
                genStructDef(dom, anal, seen, bp);
        }
    }

    private void genStructs(S3Domain dom, Analysis anal) {
        w.println("// Domain " + format(dom));
        genStructDecls(dom, anal);
        w.println("\n");
        genStructDefs(dom, anal);
    }

    public CStructGenerator(PrintWriter w, PrintWriter dw) {
        this.w = w;
        this.dw = dw;
    }
    
}
