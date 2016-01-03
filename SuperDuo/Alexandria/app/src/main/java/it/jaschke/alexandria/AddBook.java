package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
    private final int LOADER_ID = 1;
    private View rootView;
    private final String EAN_CONTENT = "eanContent";
    private static final String SCAN_FORMAT = "scanFormat";
    private static final String SCAN_CONTENTS = "scanContents";

    private String mScanFormat = "Format:";
    private String mScanContents = "Contents:";


    @Bind(R.id.ean)
    EditText bookNumber;
    @Bind(R.id.scan_button)
    View scanButton;
    @Bind(R.id.save_button)
    View saveButton;
    @Bind(R.id.delete_button)
    View deleteButton;

    @Bind(R.id.bookTitle)
    TextView bookTitleView;
    @Bind(R.id.bookSubTitle)
    TextView bookSubTitleView;
    @Bind(R.id.authors)
    TextView authorView;
    @Bind(R.id.bookCover)
    ImageView bookCoverView;
    @Bind(R.id.categories)
    TextView bookCategories;

    public AddBook() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bookNumber != null) {
            outState.putString(EAN_CONTENT, bookNumber.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ButterKnife.bind(this, rootView);

        bookNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                String ean = s.toString();
                //catch isbn10 numbers
                if (ean.length() == 10 && !ean.startsWith("978")) {
                    Log.v(TAG, "afterTextChanged Found 978+10-digit number");
                    ean = "978" + ean;
                    //? bookNumber.setText(ean);
                }
                if (ean.length() < 13) {
                    Log.v(TAG, "afterTextChanged Not enough digits in book number " + ean.length());
                    clearFields();
                    return;
                }
                Log.v(TAG, "afterTextChanged start fetch intent for " + ean);

                //Once we have an ISBN, start a book intent
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean);
                bookIntent.setAction(BookService.FETCH_BOOK);
                getActivity().startService(bookIntent);
                AddBook.this.restartLoader();
            }
        });

        if (savedInstanceState != null) {
            bookNumber.setText(savedInstanceState.getString(EAN_CONTENT));
            bookNumber.setHint("");
        }

        networkCheck();
        return rootView;
    }

    private void restartLoader() {
        if (BookService.isNetworkAvailable(getActivity())) {
            getLoaderManager().restartLoader(LOADER_ID, null, this);
        }
    }

    private void networkCheck() {
        // Don't even try to fetch the book if we have no network.
        if (!BookService.isNetworkAvailable(getActivity())) {
            bookNumber.setHint(R.string.err_no_network_to_add_book);
        }
    }

    @OnClick(R.id.scan_button)
    public void onClick(View v) {
        // This is the callback method that the system will invoke when your button is
        // clicked. You might do this by launching another app or by including the
        //functionality directly in this app.
        // Hint: Use a Try/Catch block to handle the Intent dispatch gracefully, if you
        // are using an external app.
        //when you're done, remove the toast below.
        Context context = getActivity();
        CharSequence text = "This button should let you scan a book for its barcode!";
        int duration = Toast.LENGTH_SHORT;

        // TODO: Add scan code here using library
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();

    }

    @OnClick(R.id.save_button)
    public void onSave(View view) {
        bookNumber.setText("");
    }

    @OnClick(R.id.delete_button)
    public void onDelete(View view) {
        Intent bookIntent = new Intent(getActivity(), BookService.class);
        bookIntent.putExtra(BookService.EAN, bookNumber.getText().toString());
        bookIntent.setAction(BookService.DELETE_BOOK);
        getActivity().startService(bookIntent);
        bookNumber.setText("");
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (bookNumber.getText().length() == 0) {
            return null;
        }
        String eanStr = bookNumber.getText().toString();
        if (eanStr.length() == 10 && !eanStr.startsWith("978")) {
            eanStr = "978" + eanStr;
        }
        Log.d(TAG, "onCreateLoader for " + eanStr);
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            Log.d(TAG, "onLoadFinished - no data");
            networkCheck();
            return;
        }

        Log.d(TAG, "onLoadFinished data count: " + data.getCount());
        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        bookTitleView.setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        bookSubTitleView.setText(bookSubTitle);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        String[] authorsArr = authors.split(",");
        authorView.setLines(authorsArr.length);
        authorView.setText(authors.replace(",", "\n"));
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if (Patterns.WEB_URL.matcher(imgUrl).matches()) {
            new DownloadImage(bookCoverView).execute(imgUrl);
            bookCoverView.setVisibility(View.VISIBLE);
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        bookCategories.setText(categories);

        saveButton.setVisibility(View.VISIBLE);
        deleteButton.setVisibility(View.VISIBLE);
    }


    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields() {
        bookTitleView.setText("");
        bookSubTitleView.setText("");
        authorView.setText("");
        bookCategories.setText("");
        bookCoverView.setVisibility(View.INVISIBLE);
        saveButton.setVisibility(View.INVISIBLE);
        deleteButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }
}
