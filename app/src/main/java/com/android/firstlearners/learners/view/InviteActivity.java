package com.android.firstlearners.learners.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.TextView;

import com.android.firstlearners.learners.R;
import com.android.firstlearners.learners.contract.InviteContract;
import com.android.firstlearners.learners.etc.Address;
import com.android.firstlearners.learners.etc.LearnersApplication;
import com.android.firstlearners.learners.model.NetworkService;
import com.android.firstlearners.learners.model.Repository;
import com.android.firstlearners.learners.model.SharedPreferenceManager;
import com.android.firstlearners.learners.presenter.InvitePresenter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

public class InviteActivity extends AppCompatActivity implements InviteContract.View{
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int CREATE_STUDY_ACTIVITY = 100;
    @BindView(R.id.listOfPhoneNumber) RecyclerView recyclerView;
    @BindView(R.id.selected_item) GridView gridView;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.btn_send) TextView btn_send;
    @BindView(R.id.search_edit) EditText search;
    private List<Address> addressList;
    private List<Address> selectedItems;
    private List<Boolean> flag;
    private InviteViewAdapter inviteViewAdapter;
    private GridViewAdapter gridViewAdapter;

    private View.OnClickListener inviteViewListener;
    private View.OnClickListener gridViewListener;
    private InvitePresenter presenter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite);
        ButterKnife.bind(this);

        SharedPreferenceManager sharedPreferenceManager = new SharedPreferenceManager(this);
        NetworkService networkService = ((LearnersApplication)getApplicationContext()).getNetworkService();
        Repository repository = new Repository(sharedPreferenceManager, networkService);
        presenter = new InvitePresenter(repository, this);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        addressList = new ArrayList<>();
        selectedItems = new ArrayList<>();
        flag = new ArrayList<>();
        if(checkPermission(Manifest.permission.READ_CONTACTS)){
            getPhoneNumberList();
        }
        else{
            requestPermission(new String[]{Manifest.permission.READ_CONTACTS});
        }

        inviteViewAdapter = new InviteViewAdapter(addressList);
        inviteViewAdapter.addSelectedItem(selectedItems);
        recyclerView.setAdapter(inviteViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        gridViewAdapter = new GridViewAdapter(selectedItems);
        gridView.setAdapter(gridViewAdapter);

        inviteViewListener = new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                int position = (int)v.getTag();
                boolean temp = !flag.get(position);

                if(temp){
                    Address address = addressList.get(position);
                    selectedItems.add(address);
                }else{
                    for(Address address : selectedItems){
                        if(address.position == position){
                            selectedItems.remove(address);
                            break;
                        }
                    }
                }
                flag.set(position,temp);

                inviteViewAdapter.addSelectedItem(selectedItems);
                gridViewAdapter.addItem(selectedItems);

                inviteViewAdapter.notifyDataSetChanged();
                gridViewAdapter.notifyDataSetChanged();
            }
        };

        inviteViewAdapter.setInviteViewListener(inviteViewListener);

        gridViewListener = new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                int position = (int)v.getTag();

                Address address = selectedItems.get(position);

                flag.set(address.position, false);

                selectedItems.remove(position);

                inviteViewAdapter.addSelectedItem(selectedItems);
                gridViewAdapter.addItem(selectedItems);

                gridViewAdapter.notifyDataSetChanged();
                inviteViewAdapter.notifyDataSetChanged();
            }
        };

        gridViewAdapter.setGridViewListener(gridViewListener);
    }

    private void getPhoneNumberList(){
        int count = 0;
        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                },null,null,null);

        if(cursor != null && cursor.moveToFirst()){
            do{
                String name = cursor.getString(0);
                String phoneNumber = cursor.getString(1);
                Address address = new Address(name, phoneNumber);
                address.position = count++;
                addressList.add(address);
                flag.add(false);
            }while(cursor.moveToNext());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(permissions[0].compareTo(Manifest.permission.SEND_SMS) == 0){
                        Intent intent = getIntent();
                        int activity = intent.getIntExtra("activity",0);

                        if(activity == CREATE_STUDY_ACTIVITY){
                            presenter.createStudy(
                                    intent.getStringExtra("name"),
                                    intent.getStringExtra("goal"),
                                    intent.getStringExtra("total"),
                                    intent.getStringExtra("meet"),
                                    intent.getStringExtra("start"),
                                    intent.getStringExtra("end"),
                                    selectedItems
                            );
                        }
                        else{
                            presenter.invite(selectedItems);
                        }
                        Log.d("dddd","permission");
                    }
                    else{
                        getPhoneNumberList();
                        inviteViewAdapter.notifyDataSetChanged();
                    }
                }
                break;
        }
    }

    private boolean checkPermission(String permission){
        return ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(String[] strings){
        ActivityCompat.requestPermissions(this, strings,PERMISSION_REQUEST_CODE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @OnClick(value = {R.id.btn_send, R.id.btn_clear})
    void onClick(View view){
        int id = view.getId();

        if(id == R.id.btn_send){
            Intent intent = getIntent();
            int activity = intent.getIntExtra("activity",0);

            if(activity == CREATE_STUDY_ACTIVITY){
                if(checkPermission(Manifest.permission.SEND_SMS)){
                    presenter.createStudy(
                            intent.getStringExtra("name"),
                            intent.getStringExtra("goal"),
                            intent.getStringExtra("total"),
                            intent.getStringExtra("meet"),
                            intent.getStringExtra("start"),
                            intent.getStringExtra("end"),
                            selectedItems
                    );
                }
                else{
                    requestPermission(new String[]{Manifest.permission.SEND_SMS});
                    Log.d("dddd","request");
                }

            }
            else{
                if(checkPermission(Manifest.permission.SEND_SMS)){
                    presenter.invite(selectedItems);
                }
                else{
                    requestPermission(new String[]{Manifest.permission.SEND_SMS});
                    Log.d("dddd","request");
                }
            }
        }

        else{
            search.setText("");
        }
    }

    @Override
    public void finishActivity() {
        Intent intent = getIntent();
        int activity = intent.getIntExtra("activity",0);

        if(activity == CREATE_STUDY_ACTIVITY){
            Intent finIntent = new Intent(this, MainActivity.class);
            finIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(finIntent);
        }

        finish();
    }


    @OnTextChanged(value =  R.id.search_edit, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void searchNameOrPhoneNumber(Editable editable){
        List<Address> itemSearched = new ArrayList<>();
        String string = editable.toString();

        for(Address address : addressList){
            if(address.name.contains(string) | address.phoneNumber.contains(string)){
                itemSearched.add(address);
            }
        }
        inviteViewAdapter.addItem(itemSearched);
        inviteViewAdapter.notifyDataSetChanged();
    }
}
