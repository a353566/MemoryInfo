// IRemoteService.aidl
package com.mason.memoryinfo;
import com.mason.memoryinfo.IRemoteServiceCallback;

// Declare any non-default types here with import statements

interface IRemoteService {
    // Demonstrates some basic types that you can use as parameters and return values in AIDL.
    oneway void basicTypes(int funcID, boolean isGetData, IRemoteServiceCallback callback);
}
