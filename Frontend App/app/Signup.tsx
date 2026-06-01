import PasswordInput from "@/components/PasswordInput";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { router } from "expo-router";
import { useState } from "react";
import { StyleSheet, Text, TextInput, TouchableOpacity, View } from "react-native";

export default function Signup() {

  const [name, setName] = useState("");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [Confpassword, setConfPassword] = useState("");

  const [errors, setErrors] = useState({
    name: "",
    username: "",
    email: "",
    phone: "",
    password: "",
    Confpassword: "",
  });

  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const passwordPattern = /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*]).{8,}$/;

  const handleSignup = async () => {
    let newErrors: any = {
      name: "",
      username: "",
      email: "",
      phone: "",
      password: "",
      Confpassword: "",
    };

    let hasError = false;

    if (name.trim().length < 3) {
      newErrors.name = "Name must be longer than 2 characters";
      hasError = true;
    }

    if (username.trim().length < 5) {
      newErrors.username = "Username must be at least 5 characters";
      hasError = true;
    }

    if (!emailPattern.test(email)) {
      newErrors.email = "Invalid email format";
      hasError = true;
    }

    if (phone.trim().length < 7) {
      newErrors.phone = "Phone must be at least 7 digits";
      hasError = true;
    }

    if (!passwordPattern.test(password)) {
      newErrors.password = "Weak password";
      hasError = true;
    }

    if (password !== Confpassword) {
      newErrors.Confpassword = "Passwords do not match";
      hasError = true;
    }

    setErrors(newErrors);

    if (hasError) return;

    const oldData = await AsyncStorage.getItem("Accounts");
    const accounts = oldData ? JSON.parse(oldData) : [];

    if (accounts.some((acc: any) => acc.username === username)) {
      newErrors.username = "Username already exists";
      setErrors({ ...newErrors });
      return;
    }

    if (accounts.some((acc: any) => acc.email === email)) {
      newErrors.email = "Email already exists";
      setErrors({ ...newErrors });
      return;
    }

    accounts.forEach((acc: any) => {
      acc.isCurrent = false;
    });

    const account = {
      name,
      username,
      email,
      phone,
      password,
      isCurrent: true,
    };

    accounts.push(account);

    await AsyncStorage.setItem("Accounts", JSON.stringify(accounts));

    alert("Account created successfully");

    setName("");
    setUsername("");
    setEmail("");
    setPhone("");
    setPassword("");
    setConfPassword("");

    router.replace("/Login");
  };

  return (
    <>
      <View style={styles.container}>
        <Text style={styles.title}>Signup</Text>

        <View style={styles.form}>
            <TextInput placeholder="Name" {...inputProps} value={name} onChangeText={setName} style={styles.input} />
            {errors.name ? <Text style={styles.error}>{errors.name}</Text> : null}

            <TextInput placeholder="Username" {...inputProps} value={username} onChangeText={setUsername} style={styles.input} />
            {errors.username ? <Text style={styles.error}>{errors.username}</Text> : null}

            <TextInput placeholder="Email" {...inputProps} value={email} onChangeText={setEmail} style={styles.input} />
            {errors.email ? <Text style={styles.error}>{errors.email}</Text> : null}

            <TextInput placeholder="Phone" {...inputProps} value={phone} onChangeText={setPhone} style={styles.input} />
            {errors.phone ? <Text style={styles.error}>{errors.phone}</Text> : null}

            <PasswordInput value={password} onChangeText={setPassword} placeholder="Password" />
            {errors.password ? <Text style={styles.error}>{errors.password}</Text> : null}

            <PasswordInput value={Confpassword} onChangeText={setConfPassword} placeholder="Confirm Password" />
            {errors.Confpassword ? <Text style={styles.error}>{errors.Confpassword}</Text> : null}

            <TouchableOpacity onPress={handleSignup} style={styles.button}>
            <Text style={{ color: "#fff" }}>Sign Up</Text>
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

  error: {
    color: "red",
    fontSize: 12,
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