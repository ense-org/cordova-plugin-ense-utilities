#import <Cordova/CDVPlugin.h>

@interface EnseUtilities : CDVPlugin {
}

// The hooks for our plugin commands
- (void)echo:(CDVInvokedUrlCommand *)command;
- (void)getDate:(CDVInvokedUrlCommand *)command;

@end
