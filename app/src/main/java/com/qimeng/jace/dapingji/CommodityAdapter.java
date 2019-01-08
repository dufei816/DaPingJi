package com.qimeng.jace.dapingji;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.qimeng.jace.dapingji.entity.Commodity.CommodityEntity;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class CommodityAdapter extends RecyclerView.Adapter<CommodityAdapter.ViewHolder> {


    private List<CommodityEntity> list;
    private LayoutInflater mLayoutInflater;
    private Context context;
    private Listenet listenet;

    public void setListenet(Listenet listenet) {
        this.listenet = listenet;
    }

    public interface Listenet{
        void onClick(CommodityEntity entity);
    }

    public CommodityAdapter(List<CommodityEntity> data, Context context) {
        mLayoutInflater = LayoutInflater.from(context);
        this.list = data;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root = mLayoutInflater.inflate(R.layout.item_commodity, parent, false);
        return new ViewHolder(root);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommodityEntity data = list.get(position);
        holder.tvJifen.setText("所需积分:" + data.getJf());
        holder.tvMingcheng.setText("名称:" + data.getMc());
        Glide
                .with(context)
                .load(data.getPic())
                .into(holder.image);
        holder.btnExchange.setOnClickListener(view->{
            if (listenet != null) {
                listenet.onClick(data);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.image)
        ImageView image;
        @BindView(R.id.tv_mingcheng)
        TextView tvMingcheng;
        @BindView(R.id.tv_jifen)
        TextView tvJifen;
        @BindView(R.id.btn_exchange)
        Button btnExchange;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
