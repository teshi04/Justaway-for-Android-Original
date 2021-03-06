package info.justaway;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.viewpagerindicator.CirclePageIndicator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.ButterKnife;
import butterknife.Bind;
import info.justaway.adapter.SimplePagerAdapter;
import info.justaway.fragment.ScaleImageFragment;
import info.justaway.model.TwitterManager;
import info.justaway.util.MessageUtil;
import info.justaway.util.StatusUtil;
import info.justaway.widget.ScaleImageViewPager;

/**
 * 画像の拡大表示用のActivity、かぶせて使う
 *
 * @author aska
 */
public class ScaleImageActivity extends FragmentActivity {

    @Bind(R.id.pager) ScaleImageViewPager pager;
    @Bind(R.id.symbol) CirclePageIndicator symbol;

    private ArrayList<String> imageUrls = new ArrayList<>();
    private SimplePagerAdapter simplePagerAdapter;

    public ScaleImageActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_scale_image);
        ButterKnife.bind(this);

        simplePagerAdapter = new SimplePagerAdapter(this, pager);
        symbol.setViewPager(pager);
        pager.setOffscreenPageLimit(4);

        String firstUrl;

        // インテント経由での起動をサポート
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data == null) {
                return;
            }
            firstUrl = data.toString();
        } else {
            Bundle args = intent.getExtras();
            if (args == null) {
                return;
            }

            twitter4j.Status status = (twitter4j.Status) args.getSerializable("status");
            if (status != null) {
                Integer index = args.getInt("index", 0);
                showStatus(status, index);
            }

            firstUrl = args.getString("url");
        }

        if (firstUrl == null) {
            return;
        }

        Pattern pattern = Pattern.compile("https?://twitter\\.com/\\w+/status/(\\d+)/photo/(\\d+)/?.*");
        Matcher matcher = pattern.matcher(firstUrl);
        if (matcher.find()) {
            final Long statusId = Long.valueOf(matcher.group(1));
            new AsyncTask<Void, Void, twitter4j.Status>() {
                @Override
                protected twitter4j.Status doInBackground(Void... params) {
                    try {
                        return TwitterManager.getTwitter().showStatus(statusId);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(twitter4j.Status status) {
                    if (status != null) {
                        showStatus(status, 0);
                    }
                }
            }.execute();
            return;
        }

        symbol.setVisibility(View.GONE);
        imageUrls.add(firstUrl);
        Bundle args = new Bundle();
        args.putString("url", firstUrl);
        simplePagerAdapter.addTab(ScaleImageFragment.class, args);
        simplePagerAdapter.notifyDataSetChanged();
    }

    public void showStatus(twitter4j.Status status, Integer index) {
        ArrayList<String> urls = StatusUtil.getImageUrls(status);
        if (urls.size() == 1) {
            symbol.setVisibility(View.GONE);
        }
        for (final String imageURL : urls) {
            imageUrls.add(imageURL);
            Bundle args = new Bundle();
            args.putString("url", imageURL);
            simplePagerAdapter.addTab(ScaleImageFragment.class, args);
        }
        simplePagerAdapter.notifyDataSetChanged();
        pager.setCurrentItem(index);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scale_image, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.save) {
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    int count;
                    try {
                        URL url = new URL(imageUrls.get(pager.getCurrentItem()));
                        URLConnection connection = url.openConnection();
                        connection.connect();
                        InputStream input = new BufferedInputStream(url.openStream(), 10 * 1024);
                        File root = new File(Environment.getExternalStorageDirectory(), "/Download/");
                        File file = new File(root, new Date().getTime() + ".jpg");
                        OutputStream output = new FileOutputStream(file);
                        byte data[] = new byte[1024];
                        while ((count = input.read(data)) != -1) {
                            output.write(data, 0, count);
                        }
                        output.flush();
                        output.close();
                        input.close();
                        String[] paths = {file.getPath()};
                        String[] types = {"image/jpeg"};
                        MediaScannerConnection.scanFile(getApplicationContext(), paths, types, null);
                        MessageUtil.showToast(R.string.toast_save_image_success);
                        finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    MessageUtil.showToast(R.string.toast_save_image_success);
                }
            };
            task.execute();
        }
        return true;
    }
}
