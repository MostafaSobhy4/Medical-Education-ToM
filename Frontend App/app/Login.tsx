import PasswordInput from "@/components/PasswordInput";
import { Isloggedin } from "@/context/IsLoggedIn";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { router } from "expo-router";
import { useContext, useState } from "react";
import { StyleSheet, Text, TextInput, TouchableOpacity, View } from "react-native";

export default function Login() {
  const { setIsLoggedin } = useContext(Isloggedin);

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const handleLogin = async () => {
    const data = await AsyncStorage.getItem("Accounts");
    const accounts = data ? JSON.parse(data) : [];

    const user = accounts.find(
      (acc: any) =>
        acc.username === username.trim() &&
        acc.password === password.trim()
    );

    if (user) {
      setIsLoggedin(true);

      await AsyncStorage.setItem("currentUser", JSON.stringify(user));

      router.replace('/');
    } else {
      alert("Wrong username or password");
    }
  };

  return (
    <>
      <View style={styles.container}>

        <Text style={styles.title}>Login</Text>

        <View style={styles.form}>

          <TextInput
            placeholder="User Name"
            {...inputProps}
            value={username}
            onChangeText={setUsername}
            style={styles.input}
          />

          <PasswordInput
            value={password}
            onChangeText={setPassword}
            placeholder="Password"
          />

          <TouchableOpacity
            onPress={handleLogin}
            style={styles.button}
          >
            <Text style={{ color: 'white' }}>Login</Text>
          </TouchableOpacity>

        </View>

      </View>
    </>
  );
}

const inputProps = {
  placeholderTextColor: "#999",
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    gap: 40,
    padding: 50,
    backgroundColor: "#0F1115",
  },

  title: {
    color: "white",
    fontSize: 24,
  },

  form: {
    width: "100%",
    gap: 15,
  },

  input: {
    backgroundColor: "#191d25",
    color: "white",
    padding: 12,
    borderRadius: 8,
  },

  button: {
    backgroundColor: '#6C5CE7',
    padding: 15,
    borderRadius: 12,
    alignItems: "center",
  },
});