#ifndef Utils_h
#define Utils_h

#include <string>

@interface Utils : NSObject
+ hexStringToColor:(NSString *)stringToConvert;
+ (NSDictionary*)stringToDictionary:(NSString*)string;
+ (NSString*)dictionaryToString:(id)dict;
std::string nsStringToStdString(NSString *nsString);
@end

#endif /* Utils_h */
