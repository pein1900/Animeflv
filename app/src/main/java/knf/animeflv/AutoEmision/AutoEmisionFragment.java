package knf.animeflv.AutoEmision;

import android.graphics.drawable.NinePatchDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.h6ah4i.android.widget.advrecyclerview.animator.DraggableItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.ItemShadowDecorator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;

import org.json.JSONObject;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.animeflv.JsonFactory.BaseGetter;
import knf.animeflv.JsonFactory.JsonTypes.ANIME;
import knf.animeflv.JsonFactory.OfflineGetter;
import knf.animeflv.R;
import knf.animeflv.Utils.ExecutorManager;

/**
 * Created by Jordy on 09/01/2017.
 */

public class AutoEmisionFragment extends Fragment implements OnListInteraction {
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.progress)
    ProgressBar progressBar;
    private RecyclerViewDragDropManager dragDropManager;
    private RecyclerView.Adapter wraped;
    private AutoEmisionAdapter adapter;

    public AutoEmisionFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_emision, container, false);
        ButterKnife.bind(this, view);
        asyncStart();
        return view;
    }

    private void asyncStart() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    dragDropManager = new RecyclerViewDragDropManager();
                    dragDropManager.setInitiateOnLongPress(true);
                    dragDropManager.setInitiateOnMove(false);
                    dragDropManager.setDraggingItemShadowDrawable(
                            (NinePatchDrawable) ContextCompat.getDrawable(getContext(), R.drawable.material_shadow_z3));
                    int day = getArguments().getInt("day");
                    List<EmObj> list = AutoEmisionHelper.getDayList(getArguments().getString("array"), day);
                    startVerification(list, day);
                    AutoEmisionListHolder.setList(day, list);
                    adapter = new AutoEmisionAdapter(getActivity(), list, day, AutoEmisionFragment.this);
                    wraped = dragDropManager.createWrappedAdapter(adapter);
                    final GeneralItemAnimator animator = new DraggableItemAnimator();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                            recyclerView.setAdapter(wraped);
                            recyclerView.setItemAnimator(animator);

                            if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {
                                recyclerView.addItemDecoration(new ItemShadowDecorator((NinePatchDrawable) ContextCompat.getDrawable(getContext(), R.drawable.material_shadow_z1)));
                            }
                            dragDropManager.attachRecyclerView(recyclerView);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.executeOnExecutor(ExecutorManager.getExecutor());
    }

    private void startVerification(final List<EmObj> list, final int day) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                for (final EmObj obj : list) {
                    Log.e("Emision Check", "Day: " + day + "  Title: " + obj.getTitle());
                    String off_json = OfflineGetter.getAnime(new ANIME(Integer.parseInt(obj.getAid())));
                    if (!off_json.equals("null")) {
                        try {
                            JSONObject object = new JSONObject(off_json);
                            if (!isEmision(object)) {
                                Log.e("AutoEmision", "Deleting from list: " + obj.getTitle());
                                AutoEmisionListHolder.deleteFromList(obj.getAid(), day);
                                adapter.updatelist();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        BaseGetter.getJson(getActivity(), new ANIME(Integer.parseInt(obj.getAid())), new BaseGetter.AsyncInterface() {
                            @Override
                            public void onFinish(String json) {
                                if (!json.equals("null")) {
                                    try {
                                        JSONObject object = new JSONObject(json);
                                        if (!isEmision(object)) {
                                            Log.e("AutoEmision", "Deleting from list: " + obj.getTitle());
                                            AutoEmisionListHolder.deleteFromList(obj.getAid(), day);
                                            adapter.updatelist();
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    }
                }
                return null;
            }
        }.executeOnExecutor(ExecutorManager.getExecutor());
    }

    private boolean isEmision(JSONObject object) {
        try {
            switch (object.getString("fecha_fin").trim()) {
                case "0000-00-00":
                case "prox":
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onListEdited(List<EmObj> list) {
        AutoEmisionListHolder.setList(getArguments().getInt("day"), list);
    }

    @Override
    public void onDestroyView() {
        if (dragDropManager != null) {
            dragDropManager.release();
            dragDropManager = null;
        }
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
            recyclerView.setItemAnimator(null);
            recyclerView = null;
        }
        if (wraped != null) {
            wraped = null;
        }
        super.onDestroyView();
    }
}
