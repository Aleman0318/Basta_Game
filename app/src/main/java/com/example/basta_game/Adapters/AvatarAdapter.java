package com.example.basta_game.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.basta_game.R;
import com.example.basta_game.models.Avatar;
import com.google.android.material.card.MaterialCardView;
import java.util.List;
public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarVH> {
    private final List<Avatar> listaAvatares;
    private final OnAvatarClickListener listener;
    private int posicionSeleccionada = -1;

    public interface OnAvatarClickListener {
        void onAvatarClick(Avatar avatar);
    }

    public AvatarAdapter(List<Avatar> listaAvatares, OnAvatarClickListener listener) {
        this.listaAvatares = listaAvatares;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarAdapter.AvatarVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avatar, parent, false);
        return new AvatarVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarAdapter.AvatarVH holder, int position) {
        Avatar avatar = listaAvatares.get(position);


        if (avatar.getUrlDescarga() != null && !avatar.getUrlDescarga().trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(avatar.getUrlDescarga())
                    .placeholder(android.R.color.darker_gray)
                    .into(holder.imgAvatarItem);
        } else {

            holder.imgAvatarItem.setImageResource(R.drawable.outline_person_24);
        }

        if (posicionSeleccionada == position){
            holder.cardAvatar.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
            holder.cardAvatar.setStrokeWidth(3);
            holder.imgCheck.setVisibility(View.VISIBLE);
        }
        else {
            holder.cardAvatar.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorGrisAzulado));
            holder.cardAvatar.setStrokeWidth(1);
            holder.imgCheck.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            int posicionAnterior = posicionSeleccionada;
            posicionSeleccionada = holder.getAdapterPosition();

            notifyItemChanged(posicionAnterior);
            notifyItemChanged(posicionSeleccionada);

            listener.onAvatarClick(avatar);
        });
    }

    @Override
    public int getItemCount() {
        return listaAvatares.size();
    }

    public  static class AvatarVH extends RecyclerView.ViewHolder{
        public MaterialCardView cardAvatar;
        public ImageView imgAvatarItem, imgCheck;
        public AvatarVH(@NonNull View itemView) {
            super(itemView);
            cardAvatar = itemView.findViewById(R.id.cardAvatar);
            imgAvatarItem = itemView.findViewById(R.id.imgAvatarItem);
            imgCheck = itemView.findViewById(R.id.imgCheck);
        }
    }
}
