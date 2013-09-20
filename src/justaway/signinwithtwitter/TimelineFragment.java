package justaway.signinwithtwitter;

import twitter4j.ResponseList;
import twitter4j.Status;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * タイムライン、すべての始まり
 */
public class TimelineFragment extends BaseFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        /**
         * Streamingだけだと淋しいので、初期化時にHomeTimelineを読み込む
         */
        new LoadHomeTimeline().execute();

        return view;
    }

    /**
     * ページ最上部だと自動的に読み込まれ、スクロールしていると動かないという美しい挙動
     */
    public void onStatus(Status status) {
        final ListView listView = getListView();
        final Status s = status;
        if (listView == null) {
            return;
        }
        listView.post(new Runnable() {
            @Override
            public void run() {

                // 表示している要素の位置
                int position = listView.getFirstVisiblePosition();

                // 縦スクロール位置
                View view = listView.getChildAt(0);
                int y = view != null ? view.getTop() : 0;

                // 要素を上に追加（ addだと下に追加されてしまう ）
                TwitterAdapter adapter = (TwitterAdapter) listView.getAdapter();
                adapter.insert(s, 0);

                // 少しでもスクロールさせている時は画面を動かさない様にスクロー位置を復元する
                MainActivity activity = (MainActivity) getActivity();
                if (position != 0 || y != 0) {
                    listView.setSelectionFromTop(position + 1, y);
                    activity.onNewTimeline(false);
                } else {
                    activity.onNewTimeline(true);
                }
            }
        });
    }

    private class LoadHomeTimeline extends
            AsyncTask<String, Void, ResponseList<twitter4j.Status>> {

        @Override
        protected ResponseList<twitter4j.Status> doInBackground(
                String... params) {
            try {
                MainActivity activity = (MainActivity) getActivity();
                ResponseList<twitter4j.Status> statuses = activity.getTwitter()
                        .getHomeTimeline();
                return statuses;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ResponseList<twitter4j.Status> statuses) {
            if (statuses != null) {
                ListView listView = getListView();
                TwitterAdapter adapter = (TwitterAdapter) listView.getAdapter();
                adapter.clear();
                for (twitter4j.Status status : statuses) {
                    adapter.add(status);
                }
            } else {
                MainActivity activity = (MainActivity) getActivity();
                activity.showToast("Timelineの取得に失敗しました＞＜");
            }
        }
    }
}
