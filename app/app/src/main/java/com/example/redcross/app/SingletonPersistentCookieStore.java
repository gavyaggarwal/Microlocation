package com.example.redcross.app;

import com.loopj.android.http.PersistentCookieStore;

import android.app.Activity;
import android.content.Context;

import com.example.redcross.app.MainActivity;
import static com.example.redcross.app.MainActivity.mainContext;

/**
 * Created by kbrar on 4/12/17.
 */

public class SingletonPersistentCookieStore extends PersistentCookieStore {
    private static SingletonPersistentCookieStore instance = null;

    //a private constructor so no instances can be made outside this class
    private SingletonPersistentCookieStore(Context context) {
        super(mainContext);
    }

    private SingletonPersistentCookieStore() {
        super(mainContext);
    }

    //Everytime you need an instance, call this
    public static SingletonPersistentCookieStore getInstance() {
        if(instance == null) {
            instance = new SingletonPersistentCookieStore();
        }
        return instance;
    }

    public static SingletonPersistentCookieStore getInstance(MainActivity ma) {
        return getInstance();
    }

    //Initialize this or any other variables in probably the Application class
    public void init(Context context) {}

}
