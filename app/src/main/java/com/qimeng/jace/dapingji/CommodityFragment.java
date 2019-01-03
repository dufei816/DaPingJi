package com.qimeng.jace.dapingji;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import com.qimeng.jace.dapingji.entity.PrintEntity;
import com.qimeng.jace.dapingji.entity.User;


import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
@SuppressLint({"CheckResult","SetTextI18n"})
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

    private List<PrintEntity> printEntityList = new LinkedList<>();

    public void setListener(FragmentListener listener) {
        this.listener = listener;
    }

    public interface FragmentListener {

        void onQuit();

        void onPlay(Commodity.CommodityEntity entity, User user);

    }



    public void changeVIew(int jf) {
        user.setJf(jf);
        tvJf.setText("积分：" + user.getJf());
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
                        if (entity.getJf() > user.getJf()) {
                            showNormalDialog();
                            return;
                        }
                        listener.onPlay(entity, user);
                    });
                    recyclerView.setAdapter(adapter);
                }, error -> {
                    error.printStackTrace();
                });
        return root;
    }


    private void showNormalDialog(){
        /* @setIcon 设置对话框图标
         * @setTitle 设置对话框标题
         * @setMessage 设置对话框消息提示
         * setXXX方法返回Dialog对象，因此可以链式设置属性
         */
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(getContext());
        normalDialog.setTitle("提示");
        normalDialog.setMessage("积分不足！");
        normalDialog.setPositiveButton("确定",
                (dialog, which) -> {
                    dialog.dismiss();
                });
        AlertDialog dialog = normalDialog.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
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
