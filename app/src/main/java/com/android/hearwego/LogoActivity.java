package com.android.hearwego;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LogoActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private View decorView; //full screen ?????? ??????
    private int	uiOption; //full screen ?????? ??????
    private SignInButton btn_Google; //?????? ????????? ??????
    public FirebaseAuth auth; //firebase ?????? ??????
    public GoogleSignInClient googleSignInClient;
    public GoogleApiClient googleApiClient; //?????? API ??????????????? ??????
    private static final int REQ_SIGN_GOOGLE = 100; //?????? ????????? ?????? ??????
    public FirebaseFirestore db = FirebaseFirestore.getInstance();  // ??????????????????
    public static Context context_logo;
    public String userName;
    public String userID;
    static User user;
    public FirebaseUser firebaseUser;
    public DocumentReference ref;
    public CollectionReference cref;
    final String TAG = "LogoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logo);

        ActionBar actionBar = getSupportActionBar(); //?????????(????????????) ????????????
        actionBar.hide();

        /*?????? ?????? ?????? -> ????????? ??? ??????*/
        decorView = getWindow().getDecorView();
        uiOption = getWindow().getDecorView().getSystemUiVisibility();
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH )
            uiOption |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN )
            uiOption |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT )
            uiOption |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility( uiOption );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } //gps ?????? permission request


        // GoogleSignInOptions ??????
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build();
        googleSignInClient = GoogleSignIn.getClient(LogoActivity.this, googleSignInOptions);

        // onCreate ??????????????? FirebaseAuth ????????? ?????? ???????????? ????????????
        // ????????? ?????? ????????? > signInIntent ??????
        auth = FirebaseAuth.getInstance(); //?????????????????? ?????? ?????? ?????????
        btn_Google = findViewById(R.id.btn_Google);
        btn_Google.setOnClickListener(new View.OnClickListener() { //?????? ????????? ?????? ????????? ??????
            @Override
            public void onClick(View v) {
                Log.v(TAG, "?????? LOGIN");
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
                startActivityForResult(signInIntent, REQ_SIGN_GOOGLE);
            }
        });
        // ????????? ????????? DB??? ?????? ?????? ??? ???????????? ??????



    }

    //?????? ????????? ???????????? ??? ???????????? ??????????????? ???
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQ_SIGN_GOOGLE){
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if(result.isSuccess()){ //?????? ????????? ??????????????? ??????
                Log.v(TAG, "google sign ??????, FireBase Auth.");
                GoogleSignInAccount account = result.getSignInAccount(); //?????? ????????? ????????? ?????? ?????? ?????? (?????????, ????????? ??????, ???)
                resultLogin(account); //????????? ?????? ??? ?????? ???????????? ????????? ??????
                userID = account.getId();
                userName = account.getDisplayName();
                user = new User(userName);
                Log.d(TAG, "uid is exists. : " + userID);
                db.collection("users").document(userID).get().
                        addOnSuccessListener(this::onSuccess);
                ref = db.collection("users").document(userID);
                ref.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        user = documentSnapshot.toObject(User.class);
                    }
                });

                cref = db.collection("users");
                cref.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) { //????????? ??????????????? ???????????? ????????? ????????? ?????? ?????? ????????? ????????????.
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                //document.getData() or document.getId() ?????? ?????? ????????????
                                //???????????? ????????? ??? ??????.
                                user = document.toObject(User.class);
                            }
                        } else {                    }
                    }
                });
            } else {
                Log.v(TAG, result.isSuccess() +" Google Sign In failed. Because : " + result.getStatus().toString());
            }
        }
    }

    public void addUserIfNotExists(FirebaseUser firebaseUser){

    }

    private void onSuccess(DocumentSnapshot snapShotData) {
        if (snapShotData.exists()) {
            Log.d(TAG, "uid is exists. : " + userID);
            ref.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot snapshot,
                                    @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Log.d(TAG, "Current data: " + snapshot.getData());
                    } else {
                        Log.d(TAG, "Current data: null");
                    }
                }
            });
            ref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "DocumentSnapshot data: ");
                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });
        } else {
            // add user to FireStore
            Log.d(TAG, "there is no uid. need to add data");
            db.collection("users").document(userID)
                    .set(user)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully written!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error writing document", e);
                        }
                    });
        }
    }

    //????????? ?????? ??? ?????? ???????????? ?????????
    private void resultLogin(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(),null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<AuthResult> task) { //?????? ?????? ????????? ?????? ?????? ??????
                        if(task.isSuccessful()){ //???????????? ???????????????
                            Toast.makeText(LogoActivity.this, "????????? ??????", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                            intent.putExtra("name", account.getDisplayName());
                            intent.putExtra("imageurl", String.valueOf(account.getPhotoUrl())); //String.valueOf(): ?????? ???????????? String ????????? ??????????????? ??????
                            startActivity(intent);
                        } else{ //???????????? ???????????????
                            Toast.makeText(LogoActivity.this, "????????? ??????", Toast.LENGTH_LONG).show();
                        }
                    }
                });
        context_logo = this;
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct){
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.v("??????", "ONCOMPLETE");
                       // ?????? ?????? ???
                        if (!task.isSuccessful()) {
                            Log.v("??????", "!task.isSuccessful()");
                            Toast.makeText(LogoActivity.this, "?????? ??????", Toast.LENGTH_SHORT).show();
                        }else {
                            Log.v("??????", "task.isSuccessful()");
                            firebaseUser = auth.getCurrentUser();
                            Toast.makeText(LogoActivity.this, "?????? ??????", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    @Override
    public void onConnectionFailed(@NonNull @org.jetbrains.annotations.NotNull ConnectionResult connectionResult) {
        
    }

    //???????????? ?????? ??????????????? ????????? ??????
    @Override
    public void onStart() {
        // ????????? ???????????? ??? ???????????? ?????? ??????????????? ????????? ??????
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
    }


    //????????????
    public void signOut(){
        googleApiClient.connect();
        googleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable @org.jetbrains.annotations.Nullable Bundle bundle) {
                FirebaseAuth.getInstance().signOut();
                if(googleApiClient.isConnected()){
                    Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(new ResultCallback<Status>(){
                        @Override
                        public void onResult(@NonNull @NotNull Status status) {
                            if(status.isSuccess()){
                                Log.v("??????", "???????????? ??????");
                                setResult(1);
                            }else {
                                setResult(0);
                            }
                            finish();
                        }
                    });
                }
            }
            @Override
            public void onConnectionSuspended(int i) {
                Log.v("??????", "Google API Client Connection Suspended");
                setResult(-1);
                finish();
            }
        });
    }

    //?????? ??????
    public void withdraw() {
        googleSignInClient.revokeAccess();
        auth = FirebaseAuth.getInstance();
        auth.getCurrentUser().delete();
        // DB ??????
    }
}

