package com.hardenedhunter.fouriertransform;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Song}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MySongRecyclerViewAdapter extends RecyclerView.Adapter<MySongRecyclerViewAdapter.ViewHolder> {

    private final List<Song> mValues;

    private final CustomItemClickListener clickListener;

    public MySongRecyclerViewAdapter(List<Song> items, CustomItemClickListener listener) {
        mValues = items;
        clickListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.songAuthorView.setText(mValues.get(position).getArtist());
        holder.songNameView.setText(mValues.get(position).getTitle());
        holder.itemView.setOnClickListener(v -> clickListener.onItemClick(v, position));
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView songAuthorView;
        public final TextView songNameView;
        public Song mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            songAuthorView = (TextView) view.findViewById(R.id.song_author);
            songNameView = (TextView) view.findViewById(R.id.song_name);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + songNameView.getText() + "'";
        }
    }
}