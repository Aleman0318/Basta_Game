package com.example.basta_game;
import android.app.Application;
import android.content.Context;
import androidx.test.runner.AndroidJUnitRunner;

/** Layout-only app: never starts authentication, presence, audio or the game's Firebase listeners. */
public class LayoutsRunner extends AndroidJUnitRunner {
    @Override public Application newApplication(ClassLoader cl,String name,Context context) throws InstantiationException,IllegalAccessException,ClassNotFoundException {
        return super.newApplication(cl,Application.class.getName(),context);
    }
}
