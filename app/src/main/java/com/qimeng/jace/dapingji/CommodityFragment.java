package com.qimeng.jace.dapingji;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.qimeng.jace.dapingji.entity.Commodity;
import com.qimeng.jace.dapingji.entity.User;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@SuppressLint("CheckResult")
public class CommodityFragment extends Fragment {

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.tv_quit1)
    TextView tvQuit1;
    @BindView(R.id.tv_quit2)
    TextView tvQuit2;
    @BindView(R.id.tv_name)
    TextView tvName;
    @BindView(R.id.tv_jf)
    TextView tvJf;

    Unbinder unbinder;

    private static final String KEY = "User";

    private User user;


    private FragmentListener listener;

    public void setListener(FragmentListener listener) {
        this.listener = listener;
    }

    public interface FragmentListener {

        void onQuit();

        void onPlay(Commodity.CommodityEntity entity, User user);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Bundle bundle = getArguments();
        user = (User) bundle.getSerializable(KEY);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_commodity, container, false);
        unbinder = ButterKnife.bind(this, root);
        LinearLayoutManager manager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(manager);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        tvQuit1.setOnClickListener(view -> {
            if (listener != null) {
                listener.onQuit();
            }
        });
        tvQuit2.setOnClickListener(view -> {
            if (listener != null) {
                listener.onQuit();
            }
        });


        List<Commodity.CommodityEntity> list = new ArrayList<>();
        list.add(new Commodity.CommodityEntity("名称1", 2, "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1545889578629&di=36f0024c357a63ddc2be20c68e37310d&imgtype=0&src=http%3A%2F%2Fpic1.16pic.com%2F00%2F07%2F65%2F16pic_765243_b.jpg"));
        list.add(new Commodity.CommodityEntity("名称2", 4, "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1545889578628&di=5b19166b94b763243df82b73beea2776&imgtype=0&src=http%3A%2F%2Fimg5.duitang.com%2Fuploads%2Fitem%2F201412%2F04%2F20141204151458_TE52s.thumb.700_0.jpeg"));
        list.add(new Commodity.CommodityEntity("名称3", 5, "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1545889578627&di=16959fd78ea2fc21107225341389e8b9&imgtype=0&src=http%3A%2F%2Fh.hiphotos.baidu.com%2Fzhidao%2Fwh%253D450%252C600%2Fsign%3Dbbbb7c0658b5c9ea62a60be7e0099a36%2Fdbb44aed2e738bd43c3cf83ca28b87d6277ff99a.jpg"));
//        CommodityAdapter adapter = new CommodityAdapter(list, getContext());
//        recyclerView.setAdapter(adapter);
        if (null != user && !TextUtils.isEmpty(user.getXm())) {
            tvName.setText("姓名："+user.getXm());
            tvJf.setText("积分：" + user.getJf());
        }
        HttpUtil.getInstance().getHttp().getLp()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    Log.e("Data", data.toString());
                    CommodityAdapter adapter = new CommodityAdapter(data.getLp(), getContext());
                    adapter.setListenet(entity -> {
                        listener.onPlay(entity, user);
                    });
                    recyclerView.setAdapter(adapter);
                }, error -> {
                    error.printStackTrace();
                });
        return root;
    }


    public static CommodityFragment newInstance(User user) {
        CommodityFragment fragment = new CommodityFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY, user);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
