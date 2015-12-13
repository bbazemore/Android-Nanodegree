ReadMe for Android Nanodegree Popular Movies project

This application will help you to discover the most popular movies playing.
Here's an example of what it looks like on a tablet.

![Landscape screenshot](./device-screenshot-detail-landscape.md?raw=true "Tablet Master-Detail landscape")
![Portrait screenshot](./device-screenshot-detail-landscape.md?raw=true "Tablet Master-Detail portrait")

Features
--------
  1. Display movie posters in a gridview based on Popular, Top Rated and Now Playing lists
  from The Movie Database (TMDb).
  2. When a movie is clicked display the movie release date, rating, and plot overview.
  In separate tabs display reviews (if any) and video trailers (if any).
  3. When the trailer is clicked, display it using YouTube or whatever video
  player is available on the device
  4. Allow the user to mark a movie as a favorite. Allow the user to display only their
  favorites instead of the Popular / Top Rated poster view.
  5. Store the favorites on a database on the device so the user can display the Favorites even
  when the device is in Airplane mode.
  6. On tablets and other large devices display both the movie poster grid and the details
  for the selected movie.  
  7. The Detail view uses Material Design tabs.  You can swipe right or left to switch tabs, 
  or click on the tabs.  It was tough to get the tabs into the Master-Detail view!
  8. Reasonably robust and well behaved in airplane mode.

The Movie Database TMDb
------------------------
This project depends on The Movie Database (TMDb) API.
"This product uses the TMDb API but is not endorsed or certified by TMDb."
The developer's key for TMDb has been removed from the sources posted to github.
You may obtain your own TMDb developer's key by signing up for an account at https://www.themoviedb.org,
then select the API and request a key.
Once you have a key, use it to initialize MovieDBAPIKey in the java code of this project.

Libraries Used
---------------
* Material Design support, appcompat, cardview, palette, recyclerview, support-annotations, 
* Picasso for image handling
* Retrofit, Otto, and gson for inter-process and intra-thread communication.

General design and dependencies
-------------------------------
Display movie posters in a GridView in the main view using a traditional Model View Controller
(MVC) pattern.  It requires Java SDK 1.7 and Android API 16 (Jellybean) or better.

Movie data is fetched from The Movie Database (TMDB, https://www.themoviedb.org/)
in a RESTful manner using the Retrofit type-safe HTTP client for Android and Java,
http://square.github.io/retrofit/.  Retrofit will do the fetch in a background task,
convert the returned movie data from GSON to the MovieResults class,
then callback to the UI in the main thread using the Observer pattern.  
The Retrofit Singleton is implemented in MovieDBService.

The Otto event bus, http://square.github.io/otto/, is used to efficiently
communicate between the UI and other threads.  This is implemented in DispatchTMDB.

Movie data is stored in the MovieAdapter outside of the MainFragment so that
the movie data does not need to be refetched when the View is torn down /
rebuilt when switching between portrait and landscape view.

Favorite movie data is stored in a database on the device. The ContentProvider to
the favorites is roughed out, but not complete due to the considerable amount of
time trying to get tabs to work in a Master-Detail view.

To initialize the movie list, the MainFragment requests a page of info containing
about 20 movies by sending a LoadMovies event via Otto to the DispatchTMDB which
calls TMDB in a background thread.  DispatchTMDB posts the MoviesLoaded event
when the data arrives.  The MainActivity is subscribed to this event and
updates the MovieList and the UI when the Movie data arrives in the event.  Picasso
is used to efficiently load the movie poster image into the UI View and cache images.

The MainActivityFragment sends another LoadMovies event when the user scrolls within
two screenfuls of the bottom of the list. The display can in theory hold up to 20,000
movies.  There are some optimizations to keep from asking TMDB for the same page while
it is still fetching it from the first request.

Similarly the detail view shown when a movie poster is clicked fetches the movie
detail data by sending a LoadMovieDetail event via Otto to the DispatchTMDB which
calls TMDB in a background thread.  DispatchTMDB posts the MovieDetailLoaded event
when the data arrives.  DetailActivityFragment is subscribed to this event and
updates the UI when the MovieDetail data arrives in the event.

If we were showing "popular" and the user switched to "top rated", the movies at the
top of the top rated list may not be in the popular movies fetched so far, so a
fresh set of the correct type of movies is requested.When the user asks for a different type
of movie list: popular, top rated, or now playing;
the mMovieList is cleared and a new LoadMovies event requesting page 1 is sent to
DispatchTMDB via the bus to get that type of movie list from TMDB.
    
Build Note:
-----------
You must add a file app.properties next to the ./mobile/build.gradle that contains the line:
apiKEY=<your TMDB key #>

Future:
I did not implement: the tv build.
It would be nice to add a Search feature to find a specific movie.
