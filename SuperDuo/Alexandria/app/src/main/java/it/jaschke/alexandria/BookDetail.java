package it.jaschke.alexandria;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;


public class BookDetail extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EAN_KEY = "EAN";
    private final int LOADER_ID = 10;
    private View rootView;
    private String ean;
    private String bookTitle;
    private ShareActionProvider shareActionProvider;

    @Bind(R.id.delete_button)
    View deleteButton;
    @Bind(R.id.fullBookTitle)
    TextView fullBookTitle;
    @Bind(R.id.fullBookSubTitle)
    TextView fullBookSubTitle;
    @Bind(R.id.fullBookDesc)
    TextView fullBookDescription;
    @Bind(R.id.authors)
    TextView authorView;
    @Bind(R.id.fullBookCover)
    ImageView fullBookCover;
    @Bind(R.id.categories)
    TextView bookCategories;
    @Bind(R.id.backButton)
    ImageButton backButton;
    @Nullable
    @Bind(R.id.right_container)
    FrameLayout rightContainer;

    public BookDetail() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            ean = arguments.getString(BookDetail.EAN_KEY);
            getLoaderManager().restartLoader(LOADER_ID, null, this);
        }

        rootView = inflater.inflate(R.layout.fragment_full_book, container, false);
        ButterKnife.bind(this, rootView);

        return rootView;
    }

    @OnClick(R.id.delete_button)
    public void deleteCallback() {
        Intent bookIntent = new Intent(getActivity(), BookService.class);
        bookIntent.putExtra(BookService.EAN, ean);
        bookIntent.setAction(BookService.DELETE_BOOK);
        getActivity().startService(bookIntent);
        getActivity().getSupportFragmentManager().popBackStack();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.book_detail, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);
        shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(ean)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        fullBookTitle.setText(bookTitle);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text) + bookTitle);
        shareActionProvider.setShareIntent(shareIntent);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        fullBookSubTitle.setText(bookSubTitle);

        String desc = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.DESC));
        fullBookDescription.setText(desc);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        String[] authorsArr = authors.split(",");
        authorView.setLines(authorsArr.length);
        authorView.setText(authors.replace(",", "\n"));
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if (Patterns.WEB_URL.matcher(imgUrl).matches()) {
            new DownloadImage(fullBookCover).execute(imgUrl);
            fullBookCover.setVisibility(View.VISIBLE);
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        bookCategories.setText(categories);

        if (rightContainer != null) {
            backButton.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    @Override
    public void onPause() {
        super.onDestroyView();
        if (MainActivity.IS_TABLET && rightContainer == null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }
}
