package com.hacktothefuture.hermes;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class MessageViewActivity extends ActionBarActivity implements NewMessageDialogFragment.NewMessageDialogListener {
    private static final String TAG = "MessageViewActivity";

    String m_boardId;
    List<Board> m_boards;
    private ListView m_listView;
    private List<String> m_messages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_view);

        Intent i = getIntent();
        m_boardId = i.getStringExtra(LocationCheckService.EXTRA_BOARD_ID);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocationBus.getInstance().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocationBus.getInstance().unregister(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_message_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_reply) {
            DialogFragment dialog = new NewMessageDialogFragment();
            dialog.show(getFragmentManager(), "NewMessageDialogFragment");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Subscribe
    public void onBoardUpdate(ArrayList<Board> boards) {
        if (m_boards != null) return;
        m_boards = boards;
        if (boards == null) {
            Log.e(TAG, "Boards is null");
        }
        m_listView = (ListView) findViewById(R.id.message_view_listview);
        for (Board board : boards) {
            if (board.get_id().equals(m_boardId)) {
                m_messages = board.getMessages();
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter(this, R.layout.message_list_item, m_messages) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LayoutInflater inflater = (LayoutInflater) getLayoutInflater();
                View rowView = inflater.inflate(R.layout.message_list_item, parent, false);
                TextView textView = (TextView) rowView.findViewById(R.id.message_list_textview);
                textView.setText((String)getItem(position));
                return rowView;
            }
        };
        m_listView.setAdapter(adapter);

    }

    @Override
    public void onDialogPositiveClick(final String message) {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(AppClient.API_URL)
                .build();
        AppClient.MyApp client = restAdapter.create(AppClient.MyApp.class);
        WriteMessageBundle bundle = new WriteMessageBundle();

        bundle.setBoard_id(m_boardId);
        bundle.setContent(message);
        client.writeMessage(bundle, new Callback<String>() {
            @Override
            public void success(String s, Response response) {
                Log.i(TAG, "Append message successful.");
                m_messages.add(message);
                ((ArrayAdapter)m_listView.getAdapter()).notifyDataSetChanged();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e(TAG, "Append message failed.");
            }
        });
    }

    @Override
    public void onDialogNegativeClick() {

    }
}
