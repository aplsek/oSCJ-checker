#!/usr/bin/awk -f
# Append two VM_Address arrays to a .s file named j2c_method_ranges
# and j2c_method_pointers.  The former is a sorted list of start and end
# PCs for java methods in this .s file, and the latter contains a
# pointer to the corresponding J2cCodeFragment of each method body.
# 
# Generate a second file, code_indexes, listing the UID of each method
# in j2c_method_ranges.  This file is used to initialize each
# J2cCodeFragment's index field.
#
# All this should bring us a lot closer to allowing GCC to inline away
# methods.  But I need some way to keep reflectively invoked
# nonvirtual methods alive.

function my_gensub(pat, repl, str) {
  copy = substr(str, 1, length(str));
  sub(pat, repl, copy);
#   printf("my_gensub(%s, %s, %s) => %s\n",
# 	 pat, repl, str, copy) >> "/dev/stderr";
  return copy;
}

function number_after(prefix, str) {
  match(str, prefix);
  tail = substr(str, RSTART+RLENGTH, length(str));
#   printf("number_after(%s, %s): start = %d, length = %d, tail = %s\n",
# 	 prefix, str, RSTART, RLENGTH, tail) >> "/dev/stderr";
  sub("[^0-9].*$", "", tail);
#   printf("number_after(%s, %s) => %s\n",
# 	 prefix, str, tail) >> "/dev/stderr";
  return tail;
}

BEGIN {
  methCount = 0;
  fnCount = 0;
  while (1) {
    if ((getline < "ovm_heap_exports.h") <= 0)
      break;
    if ($0 ~ /extern e_s3_services_j2c_J2cCodeFragment.*/) {
      meth=substr($3, 1, length($3) - 6);
#     printf("decl=%s\nmeth=%s\n", $0, meth) >> "/dev/stderr";
      exportedMethod[meth] = 1;
    }
  }
  printf("") > "code_indexes";
}

#/^_?_Z[0-9]+[a-zA-Z0-9_$]+:$/ {
/^[^\.][a-zA-Z0-9_$]+:$/ {
 # len = number_after("_?_Z", $0);
 # tail = my_gensub("_?_Z[0-9]+", "", $0);
 # name = substr(tail, 1, len);
 name = substr($0, 1, length($0) -1);  
#   printf("%s: len = %d, tail = %s, name = %s\n",
# 	 $0, len, tail, name) >> "/dev/stderr";
#  printf("%s: name = %s\n", $0, name) >> "/dev/stderr";
  if (name in exportedMethod) {
    methName[fnCount] = name;
#     printf("function %d is java method %d: %s\n",
# 	   methCount, fnCount, name) >> "/dev/stderr";
    methCount++;
  }
}

# some architectures add an extra "L"
/^\.?L?LFB[0-9]+:$/ {
  methNum[fnCount] = number_after(".?L?LFB", $0);
  dot = my_gensub("LFB"methNum[fnCount]":$", "", $0);
# printf("%s got LFB %d: %s\n", dot, fnCount, $0) >> "/dev/stderr";
}

/^\.?L?LFE[0-9]+:$/ {
# printf("got LFE %d: %s\n", fnCount, $0) >> "/dev/stderr";
  fnCount++;
}

{ print; }

function declare_array(name, size) {
  print "\t.data";
  print "\t.align 8";
  print "\t.globl " underscore name;
  print underscore name ":";
  
  for (i = 0; i < header_skip_bytes/4; i++) {
  
    if ((self_forwarding_offset>=0) &&     
      (self_forwarding_offset = 4*i)) {
        
        print "\t.long " underscore name;
    } else {
      print "\t.long 0";
    }
  }
  print "\t.long " size;
}

END {
  declare_array("j2c_method_ranges", 2*methCount);
  for (i = 0; i < fnCount; i++) {
    if (i in methName) {
      print "\t.long " dot "LFB" methNum[i];
      print "\t.long " dot "LFE" methNum[i];
    }
  }

  declare_array("j2c_method_pointers", methCount);
  for (i = 0; i < fnCount; i++)
    if (i in methName) 
      print "\t.long " underscore methName[i] "_code";

  for (i = 0; i < fnCount; i++)
    if (i in methName)
      print my_gensub("^.*_", "", methName[i]) >> "code_indexes";
}
