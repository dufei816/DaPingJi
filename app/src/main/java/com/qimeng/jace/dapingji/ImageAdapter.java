package com.qimeng.jace.dapingji;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.qimeng.jace.dapingji.entity.Image;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private List<Image.Pic> urls;
    private LayoutInflater mInflater;
    private Context context;

    public ImageAdapter(List<Image.Pic> pic, Context context) {
        this.urls = pic;
        this.context = context;
        mInflater = LayoutInflater.from(context);
    }

    public void putData(List<Image.Pic> data) {
        urls = data;
        notifyDataSetChanged();
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View root = mInflater.inflate(R.layout.item_image, viewGroup, false);
        ViewHolder holder = new ViewHolder(root);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        String url = urls.get(i % urls.size()).getPic();
        Glide
                .with(context)
                .load(url)
                .into(viewHolder.image);
    }

    @Override
    public int getItemCount() {
        return urls == null ? 0 : Integer.MAX_VALUE;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.image)
        ImageView image;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
