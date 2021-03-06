package knf.animeflv.Recyclers;

import android.app.Activity;
import android.preference.PreferenceManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import knf.animeflv.Directorio.AnimeClass;
import knf.animeflv.R;
import knf.animeflv.Utils.CacheManager;
import knf.animeflv.info.Helper.InfoHelper;

/**
 * Created by Jordy on 22/08/2015.
 */
public class AdapterDirAnimeNew extends RecyclerView.Adapter<AdapterDirAnimeNew.ViewHolder> {

    List<AnimeClass> Animes;
    private Activity context;
    public AdapterDirAnimeNew(Activity context, List<AnimeClass> animes) {
        this.context = context;
        this.Animes = animes;
    }

    public static String byte2HexFormatted(byte[] arr) {
        StringBuilder str = new StringBuilder(arr.length * 2);
        for (int i = 0; i < arr.length; i++) {
            String h = Integer.toHexString(arr[i]);
            int l = h.length();
            if (l == 1) h = "0" + h;
            if (l > 2) h = h.substring(l - 2, l);
            str.append(h.toUpperCase());
            if (i < (arr.length - 1)) str.append(':');
        }
        return str.toString();
    }

    @Override
    public AdapterDirAnimeNew.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).
                inflate(R.layout.item_anime_fav, parent, false);
        return new AdapterDirAnimeNew.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final AdapterDirAnimeNew.ViewHolder holder, final int position) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("is_amoled", false)) {
            holder.card.setCardBackgroundColor(context.getResources().getColor(R.color.prim));
            holder.tv_tit.setTextColor(context.getResources().getColor(R.color.blanco));
        }
        new CacheManager().mini(context,Animes.get(holder.getAdapterPosition()).getAid(),holder.iv_rel);
        holder.tv_tit.setText(Animes.get(holder.getAdapterPosition()).getNombre());
        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InfoHelper.open(
                        context,
                        new InfoHelper.SharedItem(holder.iv_rel, "img"),
                        new InfoHelper.BundleItem("aid", Animes.get(holder.getAdapterPosition()).getAid()),
                        new InfoHelper.BundleItem("title", Animes.get(holder.getAdapterPosition()).getNombre())
                );
            }
        });
    }

    @Override
    public int getItemCount() {
        return Animes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView iv_rel;
        public TextView tv_tit;
        public CardView card;

        public ViewHolder(View itemView) {
            super(itemView);
            this.iv_rel = (ImageView) itemView.findViewById(R.id.imgCardInfoRel);
            this.tv_tit = (TextView) itemView.findViewById(R.id.tv_info_rel_tit);
            this.card = (CardView) itemView.findViewById(R.id.cardRel);
        }
    }
}
