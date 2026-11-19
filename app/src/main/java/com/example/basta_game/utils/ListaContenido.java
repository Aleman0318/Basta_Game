package com.example.basta_game.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/** Small, bounded lists inside a ScrollView: measure every row without a nested recycler. */
@SuppressWarnings({"rawtypes","unchecked"})
public class ListaContenido extends LinearLayout {
    private RecyclerView.Adapter adapter;
    private int columnas=1;
    private boolean observando;
    private final List<RecyclerView.ViewHolder> filas=new ArrayList<>();
    private final RecyclerView.AdapterDataObserver cambios=new RecyclerView.AdapterDataObserver() {
        @Override public void onChanged(){mostrar();}
        @Override public void onItemRangeChanged(int inicio,int cantidad){actualizar(inicio,cantidad);}
        @Override public void onItemRangeChanged(int inicio,int cantidad,Object payload){actualizar(inicio,cantidad);}
        @Override public void onItemRangeInserted(int inicio,int cantidad){mostrar();}
        @Override public void onItemRangeRemoved(int inicio,int cantidad){mostrar();}
        @Override public void onItemRangeMoved(int desde,int hasta,int cantidad){mostrar();}
    };
    public ListaContenido(Context c,AttributeSet attrs){super(c,attrs);setOrientation(VERTICAL);}
    public void setColumnas(int n){columnas=Math.max(1,n);mostrar();}
    public void setAdapter(RecyclerView.Adapter nuevo){
        if(adapter==nuevo)return;
        if(adapter!=null){if(observando)adapter.unregisterAdapterDataObserver(cambios);liberar();}
        adapter=nuevo;observando=false;
        if(adapter!=null){adapter.registerAdapterDataObserver(cambios);observando=true;}mostrar();
    }
    private void liberar(){if(adapter!=null)for(RecyclerView.ViewHolder f:filas)adapter.onViewRecycled(f);filas.clear();removeAllViews();}
    private void mostrar(){
        liberar();if(adapter==null)return;
        LinearLayout fila=null;
        for(int i=0;i<adapter.getItemCount();i++){
            if(i%columnas==0){fila=new LinearLayout(getContext());fila.setOrientation(HORIZONTAL);addView(fila,new LayoutParams(-1,-2));}
            RecyclerView.ViewHolder holder=adapter.createViewHolder(fila,adapter.getItemViewType(i));
            LayoutParams lp=new LayoutParams((android.view.ViewGroup.MarginLayoutParams)holder.itemView.getLayoutParams());lp.width=0;lp.weight=1;
            fila.addView(holder.itemView,lp);filas.add(holder);adapter.bindViewHolder(holder,i);
        }
        requestLayout();invalidate();
    }
    private void actualizar(int inicio,int cantidad){
        if(adapter==null)return;
        if(filas.size()!=adapter.getItemCount()){mostrar();return;}
        for(int i=Math.max(0,inicio);i<Math.min(filas.size(),inicio+cantidad);i++)adapter.bindViewHolder(filas.get(i),i);
        requestLayout();invalidate();
    }
    @Override protected void onDetachedFromWindow(){if(adapter!=null && observando){adapter.unregisterAdapterDataObserver(cambios);observando=false;}super.onDetachedFromWindow();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(adapter!=null && !observando){adapter.registerAdapterDataObserver(cambios);observando=true;actualizar(0,adapter.getItemCount());}}
}
