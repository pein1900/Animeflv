package knf.animeflv.Random;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import knf.animeflv.ColorsRes;
import knf.animeflv.R;
import knf.animeflv.Utils.CacheManager;
import knf.animeflv.Utils.ExecutorManager;
import knf.animeflv.Utils.ThemeUtils;
import knf.animeflv.info.Helper.InfoHelper;

public class RandomAdapter extends RecyclerView.Adapter<RandomAdapter.ViewHolder> {

    private final int MAX_ITEMS = 10;
    private Activity activity;
    private RandomInterfaces interfaces;
    private int lastPosition = -1;
    private List<AnimeObject> aids = new ArrayList<>();
    private List<AnimeObject> list = new ArrayList<>();

    public RandomAdapter(Activity activity) {
        this.activity = activity;
        this.interfaces = (RandomInterfaces) activity;
        this.list = RandomHelper.getList(MAX_ITEMS);
    }

    @Override
    public RandomAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(activity).
                inflate(R.layout.item_random, parent, false);
        return new RandomAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final RandomAdapter.ViewHolder holder, final int position) {
        holder.card.setBackgroundColor(ThemeUtils.isAmoled(activity) ? ColorsRes.Prim(activity) : ColorsRes.Blanco(activity));
        holder.tv_tit.setTextColor(ThemeUtils.isAmoled(activity) ? ColorsRes.SecondaryTextDark(activity) : ColorsRes.SecondaryTextLight(activity));
        holder.tv_tipo.setTextColor(ThemeUtils.getAcentColor(activity));
        if (list == null) {
            final AnimeObject object = aids.get(holder.getAdapterPosition());
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    holder.tv_tit.setText(object.title);
                    holder.tv_tipo.setText(object.tid);
                    new CacheManager().mini(activity, object.aid, holder.iv_rel);
                }
            });
            doOnlySlideOut(holder, holder.getAdapterPosition());
        } else {
            if (holder.getAdapterPosition() <= lastPosition) {
                loadData(holder);
            } else {
                if (holder.card.getVisibility() == View.INVISIBLE) {
                    doSlideIn(holder, holder.getAdapterPosition());
                } else {
                    doSlideOut(holder, holder.getAdapterPosition());
                }
            }
        }
    }

    private void doSlideOut(final RandomAdapter.ViewHolder holder, final int position) {
        Animation animation = AnimationUtils.loadAnimation(activity, R.anim.out_animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                doSlideIn(holder, position);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        holder.card.startAnimation(animation);
        holder.card.setVisibility(View.INVISIBLE);
    }

    private void doOnlySlideOut(final RandomAdapter.ViewHolder holder, final int position) {
        Animation animation = AnimationUtils.loadAnimation(activity, R.anim.out_animation);
        holder.card.startAnimation(animation);
        holder.card.setVisibility(View.INVISIBLE);
    }

    private void doSlideIn(final RandomAdapter.ViewHolder holder, final int position) {
        new AsyncTask<String, String, String>() {
            @Override
            protected String doInBackground(String... strings) {
                final AnimeObject object = list.get(holder.getAdapterPosition());
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        holder.tv_tit.setText(object.title);
                        holder.tv_tipo.setText(object.tid);
                        new CacheManager().mini(activity, object.aid, holder.iv_rel);
                        holder.card.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                InfoHelper.open(
                                        activity,
                                        new InfoHelper.SharedItem(holder.iv_rel, "img"),
                                        new InfoHelper.BundleItem("title", object.title),
                                        new InfoHelper.BundleItem("aid", object.aid)
                                );
                            }
                        });
                        Animation animation = AnimationUtils.loadAnimation(activity, R.anim.in_animation);
                        animation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                                interfaces.onFinishRefresh();
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });
                        holder.card.startAnimation(animation);
                        holder.card.setVisibility(View.VISIBLE);
                        lastPosition = holder.getAdapterPosition();
                    }
                });
                return null;
            }
        }.executeOnExecutor(ExecutorManager.getExecutor());
    }

    private void loadData(final RandomAdapter.ViewHolder holder) {
        new AsyncTask<String, String, String>() {
            @Override
            protected String doInBackground(String... strings) {
                final AnimeObject object = list.get(holder.getAdapterPosition());
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        holder.tv_tit.setText(object.title);
                        holder.tv_tipo.setText(object.tid);
                        new CacheManager().mini(activity, object.aid, holder.iv_rel);
                        holder.card.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                InfoHelper.open(
                                        activity,
                                        new InfoHelper.SharedItem(holder.iv_rel, "img"),
                                        new InfoHelper.BundleItem("title", object.title),
                                        new InfoHelper.BundleItem("aid", object.aid)
                                );
                            }
                        });
                    }
                });
                return null;
            }
        }.executeOnExecutor(ExecutorManager.getExecutor());
    }

    public void onStartRefreshing() {
        lastPosition = -1;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                aids = list;
                list = null;
                notifyDataSetChanged();
            }
        });
        new AsyncTask<String, String, String>() {
            @Override
            protected String doInBackground(String... strings) {
                list = RandomHelper.getList(MAX_ITEMS);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
                return null;
            }
        }.executeOnExecutor(ExecutorManager.getExecutor());
    }

    @Override
    public int getItemCount() {
        return MAX_ITEMS;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView iv_rel;
        public TextView tv_tit;
        public TextView tv_tipo;
        public CardView card;

        public ViewHolder(View itemView) {
            super(itemView);
            this.iv_rel = (ImageView) itemView.findViewById(R.id.imgCardInfoRel);
            this.tv_tit = (TextView) itemView.findViewById(R.id.tv_info_rel_tit);
            this.tv_tipo = (TextView) itemView.findViewById(R.id.tv_info_rel_tipo);
            this.card = (CardView) itemView.findViewById(R.id.cardRel);
        }
    }

}