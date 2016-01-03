package it.jaschke.alexandria;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import it.jaschke.alexandria.api.BookListAdapter;
import it.jaschke.alexandria.api.Callback;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;


public class ListOfBooks extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private BookListAdapter bookListAdapter;
    private int position = ListView.INVALID_POSITION;

    private final int LOADER_ID = 10;

    @Bind(R.id.searchText) EditText searchText;
    @Bind(R.id.searchButton) View searchButton;
    @Bind(R.id.listOfBooks_Empty) TextView emptyText;
    @Bind(R.id.listOfBooks) ListView bookList;

    public ListOfBooks() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Cursor cursor = getActivity().getContentResolver().query(
                AlexandriaContract.BookEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );


        bookListAdapter = new BookListAdapter(getActivity(), cursor, 0);
        View rootView = inflater.inflate(R.layout.fragment_list_of_books, container, false);
        ButterKnife.bind(this, rootView);

        bookList.setEmptyView(emptyText);
        bookList.setAdapter(bookListAdapter);

        bookList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = bookListAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    ((Callback)getActivity())
                            .onItemSelected(cursor.getString(cursor.getColumnIndex(AlexandriaContract.BookEntry._ID)));
                }
            }
        });

        updateEmptyView();
        return rootView;
    }

    @OnClick(R.id.searchButton)
        public void onSearch(View v) {
            ListOfBooks.this.restartLoader();
        }

    @OnItemClick(R.id.listOfBooks)
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        Cursor cursor = bookListAdapter.getCursor();
        if (cursor != null && cursor.moveToPosition(position)) {
            ((Callback) getActivity())
                    .onItemSelected(cursor.getString(cursor.getColumnIndex(AlexandriaContract.BookEntry._ID)));
        }
    }

    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }


    private void updateEmptyView() {
        // If there are no books to show, hide the list view and
        // give the user a hint about what is going on in a text view
        if (bookList.getCount() == 0) {
            int message = R.string.empty_book_list;
            if (!BookService.isNetworkAvailable(getActivity())) {
                message = R.string.err_no_network;
            }
            emptyText.setText(message);
            emptyText.setVisibility(View.VISIBLE);
        } else {
            emptyText.setVisibility(View.GONE);
        }
    }
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        final String selection = AlexandriaContract.BookEntry.TITLE +" LIKE ? OR " + AlexandriaContract.BookEntry.SUBTITLE + " LIKE ? ";
        String searchString =searchText.getText().toString();

        if(searchString.length()>0){
            searchString = "%"+searchString+"%";
            return new CursorLoader(
                    getActivity(),
                    AlexandriaContract.BookEntry.CONTENT_URI,
                    null,
                    selection,
                    new String[]{searchString,searchString},
                    null
            );
        }

        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        bookListAdapter.swapCursor(data);
        if (position != ListView.INVALID_POSITION) {
            bookList.smoothScrollToPosition(position);
        }
        updateEmptyView();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        bookListAdapter.swapCursor(null);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.books);
    }
}
