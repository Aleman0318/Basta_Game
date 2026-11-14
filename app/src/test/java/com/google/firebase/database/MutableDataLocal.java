package com.google.firebase.database;

import com.google.firebase.database.snapshot.NodeUtilities;

// Test fixture only: SDK snapshots in memory, without FirebaseApp or network.
public final class MutableDataLocal {
    public static MutableData desde(Object valor) {
        return new MutableData(NodeUtilities.NodeFromJSON(valor));
    }
}
