#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/utsname.h>
#include <errno.h>
#include <string.h>
#include <pwd.h>
#include <time.h>

#include "config.h"

#ifdef HAVE_LANGINFO_H
#include <langinfo.h>
#endif

#include <locale.h>

#include "systemproperties.h"
#include "native_helpers.h"
#include "jtypes.h"

#if 0
#define DEBUG_SYSPROPS
#endif

#define UNKNOWN "<unascertainable>"

#ifndef MIN
#define MIN(a, b) ({							\
  typeof(a) _a = a;							\
  typeof(b) _b = b;							\
  _a < _b ? _a : _b;							\
})
#endif

/**
 * Reads the name of the default character set being used.
 * The nl_langinfo call is used if available else we try to parse the
 * locale LC_CTYPE, else if it's the C or POSIX locale then we default to
 * UTF-8 else we leave it unspecified.
 * @param buf the buffer for the path name
 * @param buflen the length of the buffer
 * @return 0 on success, ERANGE if the buffer is too small
 */
jint get_locale(char* charset, jint charsetlen,
		char* lang, jint langlen,
		char* region, jint regionlen,
		char* varient, jint varientlen) {
    char* codeset = 0;
    char* ctype = 0;

    /* we use setlocale to set the current locale based on the user's
       environment settings. I think this is the right thing to do.
       When a program starts up it has the C/POSIX default locale and
       then we use this to set up the real locale.
    */
    setlocale(LC_ALL, "");
    ctype = setlocale(LC_CTYPE, 0);
#ifdef DEBUG_SYSPROPS
    printf("User's LC_CTYPE = %s\n", ctype);
#endif

#ifdef HAVE_LANGINFO_H
    codeset = nl_langinfo(CODESET);
#ifdef DEBUG_SYSPROPS
    printf("nl_langinfo(CODESET) = %s\n", charset);
#endif
#endif

    if (codeset != NULL) {
        strncpy(charset, codeset, charsetlen);
        if (charset[charsetlen-1] == '\0') { /* full copy? */
#ifdef DEBUG_SYSPROPS
            printf("charset = %s\n", buf);
#endif
        }
        else {
#ifdef DEBUG_SYSPROPS
            printf("Buffer length %d too small -need %d\n", buflen, strlen(charset)+1);
#endif
            return ERANGE;
        }
    }


    /* the format for the locale string is: 
           language[_territory][.codeset][@modifier]
       so we look for . and @ to delimit the string
    */
    if (ctype != 0) {
        char* uscore = strchr(ctype, '_');
        char* dot = strchr(ctype, '.');
        char* at = strchr(ctype, '@');
	char *eLang = (uscore ? uscore :
		       dot ? dot :
		       at ? at :
		       ctype + strlen(ctype));
	char *eRegion = (dot ? dot :
			 at ? at :
			 ctype + strlen(ctype));
	if (uscore || dot || at) {
	    strncpy(lang, ctype, MIN(langlen, eLang - ctype));
	    if (langlen > eLang - ctype)
		lang[eLang - ctype] = 0;
	    else
		return ERANGE;
#ifdef DEBUG_SYSPROPS
	    printf("languaage = %s\n", lang);
#endif
	} else {
#ifdef DEBUG_SYSPROPS
	    printf("ctype not structured: %s\n", ctype);
#endif
	    lang[0] = 0;
	}

	if (uscore) {
	    char *bRegion = uscore + 1;
	    int lRegion = eRegion - bRegion;
	    strncpy(region, bRegion, MIN(regionlen, lRegion));
	    if (regionlen > lRegion)
		region[lRegion] = 0;
	    else
		return ERANGE;
#ifdef DEBUG_SYSPROPS
	    printf("region = %s\n", region);
#endif
	} else {
#ifdef DEBUG_SYSPROPS
	    printf("region not found in %s\n", ctype);
#endif
	    region[0] = 0;
	}

	if (at) {
	    char *bVarient = at + 1;
	    char *eVarient = ctype + strlen(ctype);
	    int lVarient = eVarient - bVarient;
	    strncpy(varient, bVarient, MIN(varientlen, lVarient));
	    if (varientlen > lVarient) 
		varient[lVarient] = 0;
	    else
		return ERANGE;
#ifdef DEBUG_SYSPROPS
	    printf("varient = %s\n", varient);
#endif
	} else {
#ifdef DEBUG_SYSPROPS
	    printf("varient not found in %s\n", ctype);
#endif
	    varient[0] = 0;
	}

	if (codeset)
	    return 0;
	
        if (dot) {
            //            printf("Found dot\n");
            if (at != 0) {
                int len = (at - dot -1);
                //                printf("Found @ - length = %d\n", len);

                if (len >= charsetlen) {
                    strncpy(charset, dot+1, charsetlen);
#ifdef DEBUG_SYSPROPS
                    printf("Buffer length %d too small -need %d\n", charsetlen, len+1);
#endif

                    return ERANGE;
                }
                else {
                    strncpy(charset, dot+1, len);
                    charset[len] = '\0';
#ifdef DEBUG_SYSPROPS
                    printf("charset = %s\n", charset);
#endif

                    return 0;
                }
            }
            else {
                strncpy(charset, dot+1, charsetlen);
                if (charset[charsetlen-1] == '\0') { /* full copy? */
#ifdef DEBUG_SYSPROPS
                    printf("charset = %s\n", charset);
#endif
                    return 0;
                }
                else {
#ifdef DEBUG_SYSPROPS
                    printf("Buffer length %d too small -need %d\n", charsetlen, strlen((dot+1))+1);
#endif
                    return ERANGE;
                }
            }
        }
        if ( (strcmp(ctype, "C") == 0) || (strcmp(ctype, "POSIX") == 0) ) {
            strncpy(charset, "UTF-8", charsetlen);
            if (charset[charsetlen-1] == '\0') { /* full copy? */
#ifdef DEBUG_SYSPROPS
                printf("charset = %s\n", charset);
#endif
                return 0;
            }
            else {
#ifdef DEBUG_SYSPROPS
                printf("Buffer length %d too small -need %d\n", charsetlen, strlen("UTF-8")+1);
#endif
                return ERANGE;
            }
        }
    }

    /* if we get here we haven't found what we are looking for */
    charset[0] = '\0';
    return 0;
}

/**
 * Read the user database for the current user to get their name
 * and home directory. Read the current working directory.
 * @param user buffer for the user name
 * @param userlen length of the name buffer
 * @param home buffer for the home directory name
 * @param homelen length of the home directory buffer
 * @param pwd buffer for the pwd name
 * @param pwdlen length of the pwd buffer
 *
 * @return zero on success, else errno. If any error occurs then any unset
 * buffer is filled with the string "<unascertainable>", or as much of that
 * as will fit into the buffer. Once an error occurs no attempt is made to
 * fill in the other data from the system.
 *
 */
jint get_user_info(char* user, jint userlen, 
                   char* home, jint homelen,
                   char* pwd, jint pwdlen) {
    int rc = 0;
    struct passwd *userinfo = NULL;

    errno = 0; // recommended before calling getpwuid
#ifndef NO_USERID
    userinfo = getpwuid(getuid());
#endif
    if (userinfo == NULL) {
#ifdef DEBUG_SYSPROPS
        fprintf(stderr, "get_user_info: getpwuid(%d) failed: %s\n", 
                (int)getuid(),
                errno == 0 ? "unknown user" : strerror(errno));
#endif
        strncpy(user, UNKNOWN, userlen);
        strncpy(home, UNKNOWN, homelen);
        strncpy(pwd, UNKNOWN, pwdlen);
        return errno == 0 ? EINVAL : errno;
    }
    else {
#ifdef DEBUG_SYSPROPS
        printf("get_user_info: getpwuid(uid) login name = %s\n"
               "              initial working dir = %s\n"
               , userinfo->pw_name, userinfo->pw_dir);
#endif
        rc = snprintf(user, userlen, "%s", userinfo->pw_name);
        if ( rc < 0) {
#ifdef DEBUG_SYSPROPS
            fprintf(stderr, "get_user_info: snprintf of name failed: %s\n", strerror(errno));
#endif
            strncpy(user, UNKNOWN, userlen);
            strncpy(home, UNKNOWN, homelen);
            strncpy(pwd, UNKNOWN, pwdlen);
            return errno;
        }
        else if (rc >= userlen) { // truncated so issue warning
            fprintf(stderr, "WARNING: get_user_info: snprintf of user truncated - increase buffer size from %d\n", userlen);
        }

        rc = snprintf(home, homelen, "%s", userinfo->pw_dir);
        if ( rc < 0) {
#ifdef DEBUG_SYSPROPS
            fprintf(stderr, "get_user_info: snprintf of home failed: %s\n", strerror(errno));
#endif
            strncpy(home, UNKNOWN, homelen);
            strncpy(pwd, UNKNOWN, pwdlen);
            return errno;

        }
        else if (rc >= homelen) { // truncated
            fprintf(stderr, "WARNING: get_user_info: snprintf of home truncated - increase buffer size from %d\n", homelen);
        }


        if (getcwd(pwd, pwdlen) == NULL) {
            if (errno == ERANGE) {
                fprintf(stderr, "WARNING: get_user_info: getcwd failed - increase buffer size from %d\n", pwdlen);
                strncpy(pwd, UNKNOWN, pwdlen);
                return ERANGE;
            }
            return errno;
        }
        else {
#ifdef DEBUG_SYSPROPS
            printf("get_user_info: getcwd = %s\n", pwd);
#endif
        }

        return 0;
    }
}



/**
 * Get the system information
 * @param os buffer for the os  name
 * @param oslen length of the os name buffer
 * @param version buffer for the os version
 * @param versionlen length of the version buffer
 * @param arch buffer for the architecture name
 * @param archlen length of the arch buffer
 *
 * @return zero on success, else errno. If any error occurs then any unset
 * buffer is filled with the string "<unascertainable>", or as much of that
 * as will fit into the buffer. Once an error occurs no attempt is made to
 * fill in the other data from the system.
 *
 */
jint get_system_info(char* os, jint oslen, 
                     char* version, jint versionlen,
                   char* arch, jint archlen) {
    struct utsname info;

    if (uname(&info) != 0) {
#ifdef DEBUG_SYSPROPS
        fprintf(stderr, "uname failed: %s\n", strerror(errno));
#endif
        strncpy(os, UNKNOWN, oslen);
        strncpy(version, UNKNOWN, versionlen);
        strncpy(arch, UNKNOWN, archlen);
        return errno;
    }
    else {
#ifdef DEBUG_SYSPROPS
        printf("get_system_info: sysname = %s\nrelease = %s\nversion = %s\n"
               "machine = %s\n", info.sysname, info.release, info.version, info.machine);
#endif
        strncpy(os, info.sysname, oslen);
        strncpy(version, info.release, versionlen);
        strncpy(arch, info.machine, archlen);
    }
    return 0;
}


/**
 * Return the full path name of where ovm executable resides. This isn't
 * exactly the same as java.home but it's all we have for now.
 *
 * @param buf the buffer for the path name
 * @param buflen the length of the buffer
 *
 * @return 0 on success, ERANGE if the buffer is shorter than argv[0]
 */
jint get_ovm_home(char* buf, jint buflen) {

    if (get_process_arg(0, buf, buflen) == buflen) {
        // FIXME: turn into an absolute path
        return 0;
    }
    else {
        return EINVAL;
    }
}

/**
 * Find the path to the system/user's temporary directory.
 * The only official way to do this in POSIX is if P_tmpdir is defined.
 *
 * @param buf the buffer for the path name
 * @param buflen the length of the buffer
 *
 * @return 0 on success, -1 if we don't have P_tmpdir defined.
 */
jint get_temp_directory(char* buf, jint buflen) {
#ifdef P_tmpdir
    strncpy(buf, P_tmpdir, buflen);
#ifdef DEBUG_SYSPROPS
    printf("get_temp_directory: using P_tmpdir = %s\n", P_tmpdir);
#endif
    return 0;
#else
    strncpy(buf, UNKNOWN, buflen);
#ifdef DEBUG_SYSPROPS
    fprintf(stderr,"WARNING: get_temp_directory: no P_tmpdir defined\n");
#endif
    return -1;
#endif
}

/**
   This method returns us a time zone id string which is in the
   form <standard zone name><GMT offset><daylight time zone name>.
   The GMT offset is in seconds, except where it is evenly divisible
   by 3600, then it is in hours.  If the zone does not observe
   daylight time, then the daylight zone name is omitted.  Examples:
   in Chicago, the timezone would be CST6CDT.  In Indianapolis 
   (which does not have Daylight Savings Time) the string would
   be EST5
   
   OVM Note: this format requires the XSI extension to POSIX tzset so that
   the timezone global variable exists. We don't have that under OSX.
   From what I can gather from the Classpath native code the required
   format is the concatenation of tzname[0] timezone and tzname[1] - though
   the Classpath code has a strange comparison to see whether tzname[0]
   and tzname[1] are not equal. 
   It is a bit weird that POSIX requires that the TZ variable have the
   form stdoffset[dst] where tzname[0] == std and tzname[1] == dst, but
   it doesn't provide a way to get offset - other than in the XSI extension.
   Under OSX we use strftime to give at least partial information. This was
   suggested by Steve Augart of IBM, one of the other Classpath developers- DH
 */
jint get_default_timezone_id(char* buf, jint buflen) {

#if !defined (TIMEZONE_IS_VAR) 
    time_t now = time(NULL);
    strftime(buf, buflen, "%Z", localtime(&now));
#else

#define OFFSET_WIDTH 6 /* -86400 <= offset <=86400 ie 6 digits */

    int len0, len1;
    tzset();
    len0 = strlen(tzname[0]);
    len1 = strlen(tzname[1]);  
    if (strcmp(tzname[0],tzname[1])==0) { 
#ifdef DEBUG_SYSPROPS
        if (len0 + OFFSET_WIDTH >= buflen) {
            printf("Buffer length %d too small -need %d\n", buflen, len0+OFFSET_WIDTH );
        }
#endif
        snprintf(buf, buflen, "%s%ld", tzname[0],((timezone%3600)==0) ? timezone/3600 : timezone);

    } 
    else { 
#ifdef DEBUG_SYSPROPS
        if (len0 + len1 + OFFSET_WIDTH >= buflen) {
            printf("Buffer length %d too small -need %d\n", buflen, len0+len1+OFFSET_WIDTH );
        }
#endif
        snprintf(buf, buflen, "%s%ld%s", tzname[0],((timezone%3600)==0)? timezone/3600 : timezone, tzname[1]);
    }

#endif  /* defined (TIMEZONE_IS_VAR) */
    
#ifdef DEBUG_SYSPROPS
    printf("get_timezone_id: %s\n", strlen(buf) > 0 ? buf : UNKNOWN);
#endif

    return 0;
}
