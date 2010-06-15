/**
 * Helper methods for printing OVM datastructures.
 **/

#include "print.h"

#include <netinet/in.h>
const int print_guard = 1700;

#if 0
void print_utf8(jint utf8Index) {
     printf("%.*s", ntohs(*(unsigned short*)&utf8Array->values[utf8Index]),
	    &utf8Array->values[utf8Index + 2]);
}
#endif

int print_arr_jbyte(struct arr_jbyte* arr) {
  int i;
  for (i = 0; i < arr->length; i++)
    putchar(arr->values[i]);
  return 0;
}


#if 0
void print_typename(struct ovm_core_TypeName* type_name) {
     /* this better be true */
     struct ovm_core_TypeName_Scalar* tn = 
	  (struct ovm_core_TypeName_Scalar*)type_name;

     if (type_name == NULL) {
	  printf("null");
	  return;
     }

     if (tn->pack != 0) {
	  print_utf8(tn->pack); 
	  printf("/");
     }
     print_utf8(tn->name);
}

void print_descriptor(struct ovm_core_repository_Descriptor_Method* desc) {
     print_typename(desc->_parent_.return_or_field_type_);
}

void print_code_fragment(ByteCode* cf) {
     printf("CF"); 
     print_descriptor((struct ovm_core_repository_Descriptor_Method*)
		      ((struct ovm_core_repository_UnboundSelector_Method*) ((struct ovm_core_repository_Bytecode*)cf)->selector_->_parent_.selector_)->descriptor_);
}
#endif

int printString(struct java_lang_String * theString) {
  if (theString == NULL) {
    printf("<NULL>\n");
    return OK;
  }
    if (theString->data == NULL) {
	printf("<null(%d)>\n", theString->count);
	return OK;
    } else {
	int i;
	char* value = theString->data->values;
	int length = theString->count;
	for (i = theString->offset; i < length; i++) {
	    putchar(value[i]);
	    if (i > print_guard) {
		printf("<trunc'd> (%d > %d)\n", i, print_guard);
		return SYSERR;
	    }
	}
	return OK;
    }
}

#if 0
void print_blueprint_of(jref ref) {
    printBlueprint((struct s3_core_domain_S3Blueprint_Scalar*)HEADER_BLUEPRINT(ref));
}


void printType(struct s3_core_domain_S3Type_Scalar* type) {
    struct ovm_core_repository_RepositoryClass * repoClass = 
	(struct ovm_core_repository_RepositoryClass*)(type->class_);
    printf("Type : ");
    print_typename(&repoClass->name_->_parent_._parent_);
    printf("\n");
}

void printBlueprint(struct s3_core_domain_S3Blueprint_Scalar* bp) {
    struct s3_core_domain_S3Type_Scalar* type =
	(struct s3_core_domain_S3Type_Scalar*)bp->type_;
    struct ovm_core_repository_RepositoryClass * repoClass = 
	(struct ovm_core_repository_RepositoryClass*)type->class_;
    printf("Blueprint : ");
    print_typename(&repoClass->name_->_parent_._parent_);
    printf("\n");
}

/**
 * Converts (or allocates) a type name to a C string
 **/
char* typename2Cstring(struct ovm_core_TypeName_Scalar* tn) {
    //FIXME
    return NULL_REFERENCE;
}


void testprint(struct arr_jbyte* arr) {
printf("test  (%.*s)", arr->length, arr->values);
}
#endif
