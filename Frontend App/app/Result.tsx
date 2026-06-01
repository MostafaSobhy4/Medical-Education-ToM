import { ImageContext } from "@/context/ImageContext";
import * as MediaLibrary from "expo-media-library";
import { router } from "expo-router";
import { useContext } from "react";
import { Image, StyleSheet, Text, TouchableOpacity, View } from "react-native";

export default function Result() {
    const { image } = useContext(ImageContext);
    const saveImage = async () => {
        if (!image) return;

        const asset = await MediaLibrary.createAssetAsync(image);
        await MediaLibrary.createAlbumAsync("AR Filter App", asset, false)

        alert("Image saved Successfully");
    };

    return (
        <View style={styles.container}>
            <Text style={styles.title}>RESULT</Text>

            {image && (
            <Image
                source={{ uri: image }}
                style={styles.image}
            />
            )}

            <Text style={styles.subtitle}>
            Me7ashesh Filter Applied
            </Text>

            <View style={styles.buttons}>

            <TouchableOpacity style={styles.saveBtn} onPress={saveImage}>
                <Text style={styles.btnText}>Save</Text>
            </TouchableOpacity>

            <TouchableOpacity
                onPress={() => router.replace('/')}
                style={styles.retryBtn}
            >
                <Text style={styles.btnText}>Try Again</Text>
            </TouchableOpacity>

            </View>

        </View>
    );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#0F1115",
    alignItems: "center",
    justifyContent: "space-evenly",
    paddingVertical: 30,
  },

  title: {
    color: "#fff",
    fontSize: 26,
    fontWeight: "bold",
    letterSpacing: 1,
  },

  image: {
    width: 280,
    height: 280,
    borderRadius: 20,
    borderWidth: 2,
    borderColor: "#6C5CE7",
  },

  subtitle: {
    color: "#aaa",
    fontSize: 16,
    marginTop: 10,
  },

  buttons: {
    flexDirection: "row",
    gap: 15,
    marginTop: 10,
  },

  saveBtn: {
    backgroundColor: "#00B894",
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 12,
  },

  retryBtn: {
    backgroundColor: "#6C5CE7",
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 12,
  },

  btnText: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "500",
  },
});