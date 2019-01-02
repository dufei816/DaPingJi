package com.qimeng.jace.dapingji;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.qimeng.jace.dapingji.entity.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@SuppressLint("CheckResult")
public class ImageFragment extends Fragment {

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;


    Unbinder unbinder;
    private Disposable image;
    private LinearLayoutManager manager;


    public static ImageFragment newInstance() {
        ImageFragment fragment = new ImageFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_image, container, false);
        unbinder = ButterKnife.bind(this, root);

        HttpUtil.getInstance().getHttp().getGg()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    Log.e("Http", data.toString());
                    ImageAdapter adapter = new ImageAdapter(data.getPic(), getContext());
                    recyclerView.setAdapter(adapter);
                }, error -> {
                    error.printStackTrace();
                });

        List<Image.Pic> list = new ArrayList<>();
        list.add(new Image.Pic(R.mipmap.aaaa, "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1545213239112&di=7592fc7398d11ab695e10da669c37aa0&imgtype=0&src=http%3A%2F%2Fim6.leaderhero.com%2Fwallpaper%2Fuser%2F201509%2F06%2Ff1c2ae87-5.jpg"));
        list.add(new Image.Pic(R.mipmap.bbb, "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1545213292520&di=afd8ac7108e1ba4414e8b3a3c88c3079&imgtype=0&src=http%3A%2F%2Fpic1.win4000.com%2Fmobile%2F2018-01-20%2F5a62d7cc8e6b5.jpg"));
        list.add(new Image.Pic(R.mipmap.ccc, "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1545213311475&di=8ced4b743827b668cfca1e0ba2f2022f&imgtype=0&src=http%3A%2F%2Fpic.qqtn.com%2Fup%2F2017-8%2F2017080315303674677.jpg"));

        manager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(manager);
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);
//        ImageAdapter adapter = new ImageAdapter(list, getContext());
//        recyclerView.setAdapter(adapter);
        return root;
    }


    @Override
    public void onStart() {
        super.onStart();
        image = Observable.interval(4, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .subscribe(l -> {
                    recyclerView.smoothScrollToPosition(manager.findFirstVisibleItemPosition() + 1);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        if (image != null) {
            image.dispose();
        }
    }
}
